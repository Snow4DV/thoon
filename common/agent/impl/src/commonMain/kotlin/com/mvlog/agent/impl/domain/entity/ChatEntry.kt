package com.mvlog.agent.impl.domain.entity

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import kotlin.time.Instant

internal sealed interface ChatEntry {

    val id: String
    val chatId: ChatId

    /** Null once hydrated from storage; a stored turn no longer knows which run wrote it. */
    val runId: AgentRunId?
    val sequence: Long

    val messageSequence: Long?
    val createdAt: Instant
    val updatedAt: Instant

    data class UserMessage(
        override val id: String,
        override val chatId: ChatId,
        override val runId: AgentRunId?,
        override val sequence: Long,
        override val messageSequence: Long?,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        val text: String,
    ) : ChatEntry

    data class AssistantMessage(
        override val id: String,
        override val chatId: ChatId,
        override val runId: AgentRunId?,
        override val sequence: Long,
        override val messageSequence: Long?,
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
        override val messageSequence: Long?,
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
        override val messageSequence: Long?,
        override val createdAt: Instant,
        override val updatedAt: Instant,
        /** Provider-assigned; pairs a result frame with its call. */
        val toolCallId: String?,
        val name: String,
        val arguments: String?,
        val status: ToolCallStatus,
    ) : ChatEntry
}
