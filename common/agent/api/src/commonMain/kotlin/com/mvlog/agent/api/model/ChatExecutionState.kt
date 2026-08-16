package com.mvlog.agent.api.model

/**
 * Derived view of what the agent is currently doing for a chat.
 *
 * Intentionally does not expose individual runs: consumers should render progress,
 * not reconstruct the execution queue.
 */
sealed interface ChatExecutionState {

    data object Idle : ChatExecutionState

    data class Working(
        val runId: AgentRunId,
        val phase: Phase,
        val queuedPrompts: Int,
    ) : ChatExecutionState {

        enum class Phase {
            /** Accepted and persisted, execution not started yet. */
            Starting,

            Running,
        }
    }

    data class Failed(
        val runId: AgentRunId,
        val message: String?,
    ) : ChatExecutionState
}
