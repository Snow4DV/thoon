package com.mvlog.agent.impl.domain.usecase

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.model.ChatSummary
import com.mvlog.agent.api.model.ChatSearchResult
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
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

/**
 * Chat use cases: the operations a screen performs, implemented against storage.
 *
 * Each implements the contract of the same name in `common:agent:api`, so the dependency runs one
 * way only — a caller depends on the use case, the use case depends on repositories.
 */
internal class ObserveChatUseCaseImpl(
    private val chatRepository: ChatRepository,
    private val runRepository: AgentRunRepository,
    private val conversationRepository: ConversationRepository,
    private val mapper: ChatStateApiMapper,
) : ObserveChatUseCase {

    /**
     * Rebuilds the timeline from durable state before observing it.
     *
     * The projection lives only as long as the process, so opening a chat after a restart would
     * otherwise show nothing — including the tool calls of a run that died before committing.
     */
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
) : ObserveChatsUseCase {

    override fun invoke(): Flow<List<ChatSummary>> =
        metadataRepository.observeAll().map { chats -> chats.map(ChatMetadata::toSummary) }
}

internal class SearchChatsUseCaseImpl(
    private val metadataRepository: ChatMetadataRepository,
) : SearchChatsUseCase {

    override fun invoke(query: String): Flow<List<ChatSearchResult>> =
        metadataRepository.search(query).map { matches ->
            matches.map {
                ChatSearchResult(
                    chat = it.chat.toSummary(),
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

/**
 * Re-runs whatever a chat is still waiting on.
 *
 * The rule for "still waiting" lives here rather than in the coordinator so that startup recovery
 * and the retry button share one definition; `AgentRunCoordinator` calls this for each chat it finds
 * at launch.
 */
internal class RetryChatUseCaseImpl(
    private val conversationRepository: ConversationRepository,
    private val runRepository: AgentRunRepository,
    private val idGenerator: IdGenerator,
) : RetryChatUseCase {

    override suspend fun invoke(chatId: ChatId): Boolean {
        // Something is already on it. Queueing a second run would answer the same prompt twice —
        // the chat mutex would serialise them, so the duplication would be visible rather than
        // merely wasteful.
        if (runRepository.observeRuns(chatId).first().any { !it.status.isTerminal }) return false

        val prompt = conversationRepository.unansweredPrompt(chatId)

        // A turn interrupted after its tools ran resumes with a blank prompt: the conversation
        // already holds the results the model was about to read, so re-asking the original question
        // would put it in the transcript twice.
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

private fun ChatMetadata.toSummary(): ChatSummary = ChatSummary(
    id = id,
    title = title,
    configId = configId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessageAt = lastMessageAt,
    lastMessagePreview = lastMessagePreview,
)

/**
 * Accepts a prompt for execution.
 *
 * Returns as soon as the prompt and its run are durable — it does not wait for, or even start,
 * the agent. Execution is picked up separately by the coordinator, which is what lets the caller
 * navigate away without losing the answer.
 */
internal class SendPromptUseCaseImpl(
    private val chatRepository: ChatRepository,
    private val runRepository: AgentRunRepository,
    private val conversationRepository: ConversationRepository,
    private val metadataRepository: ChatMetadataRepository,
    private val idGenerator: IdGenerator,
) : SendPromptUseCase {

    override suspend fun invoke(chatId: ChatId, text: String): AgentRunId {
        val runId = AgentRunId(idGenerator.newId())

        // Durable first, and deliberately so: a process that dies after this leaves a prompt that
        // recovery can see and re-queue. Dying before it loses nothing but an unrecorded keystroke.
        conversationRepository.appendUserPrompt(chatId = chatId, text = text)

        // The first thing asked names the chat. Only the first: a title that changed with every
        // prompt would be a moving target in the list, and renaming is the user's to do once
        // anything offers it.
        if (metadataRepository.get(chatId)?.title == null) {
            chatTitleFrom(text)?.let { metadataRepository.setTitle(chatId, it) }
        }

        chatRepository.appendUserMessage(chatId = chatId, runId = runId, text = text)
        runRepository.enqueue(runId = runId, chatId = chatId, prompt = text)
        return runId
    }
}

/**
 * A chat's name, taken from the first thing asked of it.
 *
 * The first line only, and trimmed: a prompt is often a paragraph, and a list row shows one line.
 * Null for a prompt with nothing in it, so a blank title is never stored — the list falls back to an
 * id-derived label, which at least tells two chats apart.
 */
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

        // A live run is stopped by interrupting its coroutine; the executor records the outcome as
        // it unwinds. A run that has not started yet has no coroutine to interrupt, so it is
        // retired directly and the coordinator will simply never pick it up.
        val interrupted = canceller.cancelRunning(runId)
        if (!interrupted && run.status == AgentRunStatus.Queued) {
            runRepository.markCancelled(runId)
        }
    }
}

internal class StartAgentRuntimeUseCaseImpl(
    private val coordinator: AgentRunCoordinator,
) : StartAgentRuntimeUseCase {

    override fun invoke() = coordinator.start()
}
