package com.mvlog.agent.impl.domain.entity

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import kotlin.time.Instant

/**
 * Internal timeline entry. Richer than the api-level `ChatItem`: it also carries which run
 * produced it and when it was last touched, both of which consumers must not depend on.
 */
internal sealed interface ChatEntry {

    val id: String
    val chatId: ChatId

    /** Null for entries not produced by a run, such as the prompt that started one. */
    val runId: AgentRunId?
    val sequence: Long
    val createdAt: Instant
    val updatedAt: Instant

    data class UserMessage(
        override val id: String,
        override val chatId: ChatId,
        override val runId: AgentRunId?,
        override val sequence: Long,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val text: String,
    ) : ChatEntry

    data class AssistantMessage(
        override val id: String,
        override val chatId: ChatId,
        override val runId: AgentRunId?,
        override val sequence: Long,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val text: String,
        val isStreaming: Boolean,
    ) : ChatEntry

    data class Reasoning(
        override val id: String,
        override val chatId: ChatId,
        override val runId: AgentRunId?,
        override val sequence: Long,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val text: String,
        val isStreaming: Boolean,
    ) : ChatEntry

    data class ToolCall(
        override val id: String,
        override val chatId: ChatId,
        override val runId: AgentRunId?,
        override val sequence: Long,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        /** Provider-assigned id, used to correlate a result frame with its request. */
        val toolCallId: String?,
        val name: String,
        val arguments: String?,
        val status: ToolCallStatus,
    ) : ChatEntry
}
