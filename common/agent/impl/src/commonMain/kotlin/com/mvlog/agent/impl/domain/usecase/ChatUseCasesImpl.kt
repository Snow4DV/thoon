package com.mvlog.agent.impl.domain.usecase

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.model.ChatSummary
import com.mvlog.agent.api.model.ChatSearchResult
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CancelChatRunUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatsUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.agent.api.usecase.StartAgentRuntimeUseCase
import com.mvlog.agent.impl.mapper.ChatStateApiMapper
import com.mvlog.agent.impl.domain.entity.AgentRunStatus
import com.mvlog.agent.impl.domain.entity.ChatMetadata
import com.mvlog.agent.impl.domain.execution.AgentRunCanceller
import com.mvlog.agent.impl.domain.repository.AgentRunRepository
import com.mvlog.agent.impl.domain.repository.ChatMetadataRepository
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.domain.repository.ConversationRepository
import com.mvlog.agent.impl.execution.AgentRunCoordinator
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class ObserveChatUseCaseImpl(
    private val chatRepository: ChatRepository,
    private val runRepository: AgentRunRepository,
    private val conversationRepository: ConversationRepository,
    private val mapper: ChatStateApiMapper,
) : ObserveChatUseCase {

    override fun invoke(chatId: ChatId): Flow<ChatState> = flow {
        chatRepository.hydrate(chatId, conversationRepository.timeline(chatId))

        emitAll(
            combine(
                chatRepository.observeEntries(chatId),
                runRepository.observeRuns(chatId),
            ) { entries, runs ->
                mapper.map(chatId = chatId, entries = entries, runs = runs)
            }
        )
    }
}

internal class ObserveChatsUseCaseImpl(
    private val metadataRepository: ChatMetadataRepository,
    private val runRepository: AgentRunRepository,
) : ObserveChatsUseCase {

    override fun invoke(): Flow<List<ChatSummary>> =
        combine(
            metadataRepository.observeAll(),
            runRepository.observeChatsWithActiveRuns(),
        ) { chats, active ->
            chats.map { it.toSummary(isWorking = it.id in active) }
        }
}

internal class SearchChatsUseCaseImpl(
    private val metadataRepository: ChatMetadataRepository,
    private val runRepository: AgentRunRepository,
) : SearchChatsUseCase {

    override fun invoke(query: String): Flow<List<ChatSearchResult>> =
        combine(
            metadataRepository.search(query),
            runRepository.observeChatsWithActiveRuns(),
        ) { matches, active ->
            matches.map {
                ChatSearchResult(
                    chat = it.chat.toSummary(isWorking = it.chat.id in active),
                    snippet = it.snippet,
                    messageSequence = it.messageSequence,
                )
            }
        }
}

internal class CreateChatUseCaseImpl(
    private val metadataRepository: ChatMetadataRepository,
    private val idGenerator: IdGenerator,
) : CreateChatUseCase {

    override suspend fun invoke(configId: AgentConfigId?): ChatId {
        val chatId = ChatId(idGenerator.newId())
        metadataRepository.create(chatId = chatId, configId = configId)
        return chatId
    }
}

internal class RetryChatUseCaseImpl(
    private val conversationRepository: ConversationRepository,
    private val runRepository: AgentRunRepository,
    private val idGenerator: IdGenerator,
) : RetryChatUseCase {

    override suspend fun invoke(chatId: ChatId): Boolean {
        // A live run already owns this prompt; a second would answer it twice.
        if (runRepository.observeRuns(chatId).first().any { !it.status.isTerminal }) return false

        val prompt = conversationRepository.unansweredPrompt(chatId)

        // Blank prompt: the tool results are already in the conversation.
        if (prompt == null && !conversationRepository.hasUnfinishedToolTurn(chatId)) return false

        runRepository.enqueue(
            runId = AgentRunId(idGenerator.newId()),
            chatId = chatId,
            prompt = prompt.orEmpty(),
        )
        return true
    }
}

internal class DeleteChatUseCaseImpl(
    private val metadataRepository: ChatMetadataRepository,
) : DeleteChatUseCase {

    override suspend fun invoke(chatId: ChatId) = metadataRepository.delete(chatId)
}

private fun ChatMetadata.toSummary(isWorking: Boolean): ChatSummary = ChatSummary(
    id = id,
    title = title,
    configId = configId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessageAt = lastMessageAt,
    lastMessagePreview = lastMessagePreview,
    isWorking = isWorking,
)

internal class SendPromptUseCaseImpl(
    private val chatRepository: ChatRepository,
    private val runRepository: AgentRunRepository,
    private val conversationRepository: ConversationRepository,
    private val metadataRepository: ChatMetadataRepository,
    private val idGenerator: IdGenerator,
) : SendPromptUseCase {

    override suspend fun invoke(chatId: ChatId, text: String): AgentRunId {
        val runId = AgentRunId(idGenerator.newId())

        conversationRepository.appendUserPrompt(chatId = chatId, text = text)

        if (metadataRepository.get(chatId)?.title == null) {
            chatTitleFrom(text)?.let { metadataRepository.setTitle(chatId, it) }
        }

        chatRepository.appendUserMessage(chatId = chatId, runId = runId, text = text)
        runRepository.enqueue(runId = runId, chatId = chatId, prompt = text)
        return runId
    }
}

/** First non-blank line, truncated. Null for a blank prompt so the id-derived fallback survives. */
internal fun chatTitleFrom(prompt: String): String? {
    val firstLine = prompt.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: return null

    return if (firstLine.length <= CHAT_TITLE_LENGTH) {
        firstLine
    } else {
        firstLine.take(CHAT_TITLE_LENGTH).trimEnd() + "…"
    }
}

private const val CHAT_TITLE_LENGTH = 60

internal class CancelAgentRunUseCaseImpl(
    private val runRepository: AgentRunRepository,
    private val canceller: AgentRunCanceller,
) : CancelAgentRunUseCase {

    override suspend fun invoke(runId: AgentRunId) {
        val run = runRepository.get(runId) ?: return
        if (run.status.isTerminal) return

        // A queued run has no coroutine to interrupt: retire the row and the coordinator never
        // picks it up.
        val interrupted = canceller.cancelRunning(runId)
        if (!interrupted && run.status == AgentRunStatus.Queued) {
            runRepository.markCancelled(runId)
        }
    }
}

internal class CancelChatRunUseCaseImpl(
    private val runRepository: AgentRunRepository,
    private val cancelRun: CancelAgentRunUseCase,
) : CancelChatRunUseCase {

    override suspend fun invoke(chatId: ChatId) {
        // Queued first: the worker picks the next run the moment the live one ends, so cancelling
        // the live one first would hand the queue a head start.
        runRepository.observeRuns(chatId).first()
            .filter { !it.status.isTerminal }
            .sortedBy { it.status == AgentRunStatus.Running }
            .forEach { cancelRun(it.id) }
    }
}

internal class StartAgentRuntimeUseCaseImpl(
    private val coordinator: AgentRunCoordinator,
) : StartAgentRuntimeUseCase {

    override fun invoke() = coordinator.start()
}
