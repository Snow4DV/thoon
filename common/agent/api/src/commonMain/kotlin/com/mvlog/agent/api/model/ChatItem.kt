package com.mvlog.agent.api.model

import kotlin.time.Instant

/**
 * A single entry of the agent conversation timeline, as seen by feature modules.
 */
sealed interface ChatItem {

    val id: String

    val createdAt: Instant

    data class UserMessage(
        override val id: String,
        override val createdAt: Instant,
        val text: String,
    ) : ChatItem

    data class AssistantMessage(
        override val id: String,
        override val createdAt: Instant,
        val text: String,
        val isStreaming: Boolean,
    ) : ChatItem

    /**
     * Reasoning emitted by the provider itself, not internal agent bookkeeping.
     */
    data class Reasoning(
        override val id: String,
        override val createdAt: Instant,
        val text: String,
        val isStreaming: Boolean,
    ) : ChatItem

    data class ToolCall(
        override val id: String,
        override val createdAt: Instant,
        val name: String,
        val arguments: String?,
        val state: ToolCallState,
    ) : ChatItem
}
