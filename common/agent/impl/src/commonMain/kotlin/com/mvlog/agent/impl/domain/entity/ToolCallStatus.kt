package com.mvlog.agent.impl.domain.entity

internal sealed interface ToolCallStatus {

    data object Pending : ToolCallStatus

    data object AwaitingApproval : ToolCallStatus

    data object Running : ToolCallStatus

    data class Completed(val result: String) : ToolCallStatus

    data class Failed(val error: String) : ToolCallStatus
}
