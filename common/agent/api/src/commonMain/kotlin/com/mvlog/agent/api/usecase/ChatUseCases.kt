package com.mvlog.agent.api.usecase

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatState
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

/** Creates a durable, empty chat. A null configuration leaves it following the default. */
fun interface CreateChatUseCase {
    suspend operator fun invoke(configId: AgentConfigId?): ChatId
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
