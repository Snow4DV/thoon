package com.mvlog.agent.api.model

/**
 * Per-chat progress; deliberately hides individual runs — render progress, do not rebuild the
 * queue.
 */
sealed interface ChatExecutionState {

    data object Idle : ChatExecutionState

    data class Working(
        val runId: AgentRunId,
        val phase: Phase,
        val queuedPrompts: Int,
    ) : ChatExecutionState {

        enum class Phase {
            /** Persisted; no worker has picked it up yet. */
            Starting,

            Running,
        }
    }

    /** No run is live; a prompt sent now would follow an unanswered tool call, so sending is off. */
    data class AwaitingApproval(val undecidedCalls: Int) : ChatExecutionState

    data class Failed(
        val runId: AgentRunId,
        val message: String?,
    ) : ChatExecutionState
}
