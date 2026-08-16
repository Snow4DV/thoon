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

/**
 * The conversation as the UI renders it.
 *
 * An in-memory projection, not a store: entries are rebuilt from committed history and the latest
 * checkpoint. Nothing here needs to survive the process.
 */
internal interface ChatRepository {

    /** Replaces the projection for a chat, after rebuilding it from durable state. */
    suspend fun hydrate(chatId: ChatId, entries: List<ChatEntry>)

    fun observeEntries(chatId: ChatId): Flow<List<ChatEntry>>

    /** Shows the prompt in the timeline. Durability is the conversation's job, not this one's. */
    suspend fun appendUserMessage(chatId: ChatId, runId: AgentRunId, text: String)

    suspend fun beginStreamingEntry(
        chatId: ChatId,
        runId: AgentRunId,
        kind: StreamingEntryKind,
    ): String

    /**
     * Replaces the whole text of a streaming entry.
     *
     * Absolute rather than delta-append: the event sink buffers deltas in memory and flushes the
     * accumulated text periodically, so a dropped or replayed flush cannot corrupt the entry.
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

    /** Resolves a provider-assigned tool call id back to the entry created for it. */
    suspend fun findToolCallEntryId(chatId: ChatId, toolCallId: String): String?

    /**
     * Marks any entry still flagged as streaming for [runId] as finished.
     *
     * Called when a run ends for any reason, so a killed or failed run cannot leave the timeline
     * showing a permanently spinning message.
     */
    suspend fun settleStreamingEntries(runId: AgentRunId)
}
