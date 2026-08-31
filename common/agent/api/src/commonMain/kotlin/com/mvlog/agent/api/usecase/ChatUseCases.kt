package com.mvlog.agent.api.usecase

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.model.ChatSearchResult
import com.mvlog.agent.api.model.ChatSummary
import kotlinx.coroutines.flow.Flow

/**
 * Everything a chat screen needs.
 *
 * Declared one operation at a time rather than as a single service so a screen depends only on
 * what it uses, and so each can be substituted individually in tests and previews.
 */

/**
 * Emits the current timeline and execution state of a chat, and every subsequent change.
 *
 * Backed by storage rather than by an in-flight run: leaving and re-entering a screen restores
 * whatever the agent produced in the meantime.
 */
fun interface ObserveChatUseCase {
    operator fun invoke(chatId: ChatId): Flow<ChatState>
}

/**
 * Every chat, most recently updated first.
 *
 * Summaries rather than full chats: a list should not pay for conversations it is not showing.
 */
fun interface ObserveChatsUseCase {
    operator fun invoke(): Flow<List<ChatSummary>>
}

/** Creates a durable, empty chat. A null configuration leaves it following the default. */
fun interface CreateChatUseCase {
    suspend operator fun invoke(configId: AgentConfigId?): ChatId
}

/**
 * Removes a chat along with the conversation and checkpoints behind it.
 *
 * A run already executing for this chat is not interrupted. It finishes against storage that no
 * longer accepts it and is discarded — wasteful, but not corrupting.
 */
/**
 * Chats whose title or messages contain [query], newest conversation first.
 *
 * Matches what people said, not what the model thought: reasoning traces and tool output are stored
 * but never searched. A blank query returns every chat, so a search field can drive this directly.
 */
fun interface SearchChatsUseCase {
    operator fun invoke(query: String): Flow<List<ChatSearchResult>>
}

/**
 * Runs again whatever this chat is still waiting on.
 *
 * That is a prompt nothing answered, or a turn interrupted after its tools ran — the same two things
 * startup recovery looks for, so a tapped retry and a relaunch can never disagree about what counts
 * as unfinished.
 *
 * Returns false when there was nothing to run, or when something already is: a caller that cannot
 * tell those from success would leave the user waiting on a reply nobody queued.
 */
fun interface RetryChatUseCase {
    suspend operator fun invoke(chatId: ChatId): Boolean
}

fun interface DeleteChatUseCase {
    suspend operator fun invoke(chatId: ChatId)
}

/**
 * Persists a prompt and accepts it for execution.
 *
 * Returns once the prompt and its queued run are durable — not when the agent has answered. The
 * answer arrives through [ObserveChatUseCase].
 */
fun interface SendPromptUseCase {
    suspend operator fun invoke(chatId: ChatId, text: String): AgentRunId
}

/** Stops a run, whether it is still queued or already executing. */
fun interface CancelAgentRunUseCase {
    suspend operator fun invoke(runId: AgentRunId)
}

/** The configuration a chat will run on: its override, or the default. */
fun interface ObserveChatConfigUseCase {
    operator fun invoke(chatId: ChatId): Flow<AgentConfig?>
}

/** Pins a chat to a configuration, or clears the pin so it follows the default again. */
fun interface SetChatConfigUseCase {
    suspend operator fun invoke(chatId: ChatId, configId: AgentConfigId?): AgentConfigResult<Unit>
}
