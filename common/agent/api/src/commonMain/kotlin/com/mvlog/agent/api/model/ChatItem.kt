package com.mvlog.agent.api.model

import kotlin.time.Instant

/**
 * A single entry of the agent conversation timeline, as seen by feature modules.
 */
sealed interface ChatItem {

    val id: String

    /**
     * Which stored turn this came from, or null while a run is still producing it.
     *
     * Several items can share one value — a turn holding reasoning, a reply and a tool call becomes
     * three. It exists so a search result, which knows a message and not a rendered item, can say
     * which part of a conversation it means; [id] is a counter over visible items and cannot.
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

    /**
     * Reasoning emitted by the provider itself, not internal agent bookkeeping.
     */
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
}
