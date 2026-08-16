package com.mvlog.agent.impl.domain.usecase

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.agent.api.usecase.StartAgentRuntimeUseCase
import com.mvlog.agent.impl.mapper.ChatStateApiMapper
import com.mvlog.agent.impl.domain.entity.AgentRunStatus
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
import kotlinx.coroutines.flow.flow

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
    private val idGenerator: IdGenerator,
) : SendPromptUseCase {

    override suspend fun invoke(chatId: ChatId, text: String): AgentRunId {
        val runId = AgentRunId(idGenerator.newId())

        // Durable first, and deliberately so: a process that dies after this leaves a prompt that
        // recovery can see and re-queue. Dying before it loses nothing but an unrecorded keystroke.
        conversationRepository.appendUserPrompt(chatId = chatId, text = text)

        chatRepository.appendUserMessage(chatId = chatId, runId = runId, text = text)
        runRepository.enqueue(runId = runId, chatId = chatId, prompt = text)
        return runId
    }
}

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
