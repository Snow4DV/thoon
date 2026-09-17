package com.mvlog.agent.api.usecase

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.model.ChatSearchResult
import com.mvlog.agent.api.model.ChatSummary
import kotlinx.coroutines.flow.Flow

/** Backed by storage: re-entering a chat shows whatever the run produced meanwhile. */
fun interface ObserveChatUseCase {
    operator fun invoke(chatId: ChatId): Flow<ChatState>
}

/** Every chat, most recent conversation first. */
fun interface ObserveChatsUseCase {
    operator fun invoke(): Flow<List<ChatSummary>>
}

/** Creates a durable, empty chat. A null configuration leaves it following the default. */
fun interface CreateChatUseCase {
    suspend operator fun invoke(configId: AgentConfigId?): ChatId
}

/** Title or user/assistant text, newest conversation first. A blank query lists every chat. */
fun interface SearchChatsUseCase {
    operator fun invoke(query: String): Flow<List<ChatSearchResult>>
}

/**
 * Re-runs an unanswered prompt or a turn interrupted after its tools ran. False when there was
 * nothing to run, or a run is already active.
 */
fun interface RetryChatUseCase {
    suspend operator fun invoke(chatId: ChatId): Boolean
}

/**
 * Removes the chat, its conversation and checkpoints. A run in flight is not cancelled; its result
 * is discarded.
 */
fun interface DeleteChatUseCase {
    suspend operator fun invoke(chatId: ChatId)
}

/**
 * Returns once the prompt and its run are durable, not when answered; the answer arrives via
 * [ObserveChatUseCase].
 */
fun interface SendPromptUseCase {
    suspend operator fun invoke(chatId: ChatId, text: String): AgentRunId
}

/** Stops a run, whether it is still queued or already executing. */
fun interface CancelAgentRunUseCase {
    suspend operator fun invoke(runId: AgentRunId)
}

/** Stops whatever the chat is doing: the executing run and anything queued behind it. */
fun interface CancelChatRunUseCase {
    suspend operator fun invoke(chatId: ChatId)
}

/** The chat's explicit override. Null means it follows the default. */
fun interface ObserveChatConfigOverrideUseCase {
    operator fun invoke(chatId: ChatId): Flow<AgentConfigId?>
}

/** Sets the override; null clears it so the chat follows the default. */
fun interface SetChatConfigUseCase {
    suspend operator fun invoke(chatId: ChatId, configId: AgentConfigId?): AgentConfigResult<Unit>
}
