package com.mvlog.agent.api.model

/**
 * Lifecycle of a single tool invocation.
 *
 * Terminal states carry their payload so consumers can render a result or an error
 * without consulting a separate nullable field.
 */
sealed interface ToolCallState {

    /** Requested by the model, not started yet. */
    data object Pending : ToolCallState

    data object Running : ToolCallState

    data class Completed(val result: String) : ToolCallState

    data class Failed(val error: String) : ToolCallState
}
