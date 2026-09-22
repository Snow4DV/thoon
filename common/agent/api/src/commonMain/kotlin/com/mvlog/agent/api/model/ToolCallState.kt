package com.mvlog.agent.api.model

sealed interface ToolCallState {

    /** Requested by the model, not started yet. */
    data object Pending : ToolCallState

    /** The run stopped for a decision; a [ChatItem.ToolApprovalRequest] follows this call. */
    data object AwaitingApproval : ToolCallState

    data object Running : ToolCallState

    data class Completed(val result: String) : ToolCallState

    data class Failed(val error: String) : ToolCallState
}
