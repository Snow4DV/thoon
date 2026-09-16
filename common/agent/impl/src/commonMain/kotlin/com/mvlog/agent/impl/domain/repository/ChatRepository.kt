package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.entity.ToolCallStatus
import kotlinx.coroutines.flow.Flow

internal enum class StreamingEntryKind {
    Assistant,
    Reasoning,
}

internal interface ChatRepository {

    suspend fun hydrate(chatId: ChatId, entries: List<ChatEntry>)

    fun observeEntries(chatId: ChatId): Flow<List<ChatEntry>>

    suspend fun appendUserMessage(chatId: ChatId, runId: AgentRunId, text: String)

    suspend fun beginStreamingEntry(
        chatId: ChatId,
        runId: AgentRunId,
        kind: StreamingEntryKind,
    ): String

    /**
     * Whole text, not a delta: the sink flushes its buffer on a timer, so a replayed flush is
     * harmless.
     */
    suspend fun updateStreamingText(entryId: String, text: String)

    suspend fun completeStreamingEntry(entryId: String, finalText: String)

    suspend fun addToolCall(
        chatId: ChatId,
        runId: AgentRunId,
        toolCallId: String?,
        name: String,
        arguments: String?,
    ): String

    suspend fun updateToolCallStatus(entryId: String, status: ToolCallStatus)

    suspend fun findToolCallEntryId(chatId: ChatId, toolCallId: String): String?

    /** Called however a run ends, so a failed or killed run never leaves a spinner. */
    suspend fun settleStreamingEntries(runId: AgentRunId)
}
