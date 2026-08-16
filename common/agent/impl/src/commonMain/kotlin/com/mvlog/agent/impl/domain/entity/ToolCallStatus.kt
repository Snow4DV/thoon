package com.mvlog.agent.impl.domain.entity

/**
 * Internal counterpart of the api-level `ToolCallState`.
 *
 * Kept separate so the stored/execution representation can grow fields the contract should not
 * expose (raw payloads, retry counts) without forcing an api change.
 */
internal sealed interface ToolCallStatus {

    data object Pending : ToolCallStatus

    data object Running : ToolCallStatus

    data class Completed(val result: String) : ToolCallStatus

    data class Failed(val error: String) : ToolCallStatus
}
