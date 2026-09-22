package com.mvlog.agent.api.model

import kotlin.time.Instant

sealed interface ChatItem {

    val id: String

    /**
     * The stored turn this came from; null while still streaming. Every item from one turn shares
     * it.
     */
    val messageSequence: Long?

    val createdAt: Instant

    data class UserMessage(
        override val id: String,
        override val messageSequence: Long?,
        override val createdAt: Instant,
        val text: String,
    ) : ChatItem

    data class AssistantMessage(
        override val id: String,
        override val messageSequence: Long?,
        override val createdAt: Instant,
        val text: String,
        val isStreaming: Boolean,
    ) : ChatItem

    /** The provider's own reasoning stream, not agent bookkeeping. */
    data class Reasoning(
        override val id: String,
        override val messageSequence: Long?,
        override val createdAt: Instant,
        val text: String,
        val isStreaming: Boolean,
    ) : ChatItem

    data class ToolCall(
        override val id: String,
        override val messageSequence: Long?,
        override val createdAt: Instant,
        val name: String,
        val arguments: String?,
        val state: ToolCallState,
    ) : ChatItem

    /** Follows the [ToolCall] it gates while the run is stopped for a decision. */
    data class ToolApprovalRequest(
        override val id: String,
        override val messageSequence: Long?,
        override val createdAt: Instant,
        /** Stable within the chat; hand it back to DecideToolCallUseCase. */
        val toolCallKey: String,
        val toolName: String,
        val arguments: String?,
        /** Null until answered; set while other calls in the turn are still undecided. */
        val decision: ToolApprovalDecision?,
    ) : ChatItem
}
