package com.mvlog.agent.impl.data.memory

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.entity.ToolCallStatus
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.domain.repository.StreamingEntryKind
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class InMemoryChatRepository(
    private val store: InMemoryAgentStore,
    private val clock: AgentClock,
    private val idGenerator: IdGenerator,
) : ChatRepository {

    override suspend fun hydrate(chatId: ChatId, entries: List<ChatEntry>) {
        store.transaction { store.replaceEntries(chatId, entries) }
    }

    override fun observeEntries(chatId: ChatId): Flow<List<ChatEntry>> =
        store.entries
            .map { it[chatId].orEmpty() }
            .distinctUntilChanged()

    override suspend fun appendUserMessage(chatId: ChatId, runId: AgentRunId, text: String) {
        val now = clock.now()
        store.transaction {
            store.putEntry(
                ChatEntry.UserMessage(
                    id = idGenerator.newId(),
                    chatId = chatId,
                    runId = runId,
                    sequence = store.nextSequence(chatId),
                    // Null while the run is in flight: this entry has no stored turn yet, and the
                    // hydrate that follows the commit replaces it with an anchored one.
                    messageSequence = null,
                    createdAt = now,
                    updatedAt = now,
                    text = text,
                )
            )
        }
    }

    override suspend fun beginStreamingEntry(
        chatId: ChatId,
        runId: AgentRunId,
        kind: StreamingEntryKind,
    ): String {
        val id = idGenerator.newId()
        val now = clock.now()
        store.transaction {
            val sequence = store.nextSequence(chatId)
            store.putEntry(
                when (kind) {
                    StreamingEntryKind.Assistant -> ChatEntry.AssistantMessage(
                        id = id,
                        chatId = chatId,
                        runId = runId,
                        sequence = sequence,
                        messageSequence = null,
                        createdAt = now,
                        updatedAt = now,
                        text = "",
                        isStreaming = true,
                    )

                    StreamingEntryKind.Reasoning -> ChatEntry.Reasoning(
                        id = id,
                        chatId = chatId,
                        runId = runId,
                        sequence = sequence,
                        messageSequence = null,
                        createdAt = now,
                        updatedAt = now,
                        text = "",
                        isStreaming = true,
                    )
                }
            )
        }
        return id
    }

    override suspend fun updateStreamingText(entryId: String, text: String) {
        updateStreaming(entryId, text, isStreaming = true)
    }

    override suspend fun completeStreamingEntry(entryId: String, finalText: String) {
        updateStreaming(entryId, finalText, isStreaming = false)
    }

    private suspend fun updateStreaming(entryId: String, text: String, isStreaming: Boolean) {
        val now = clock.now()
        store.transaction {
            when (val entry = store.findEntry(entryId)) {
                is ChatEntry.AssistantMessage ->
                    store.putEntry(entry.copy(text = text, isStreaming = isStreaming, updatedAt = now))

                is ChatEntry.Reasoning ->
                    store.putEntry(entry.copy(text = text, isStreaming = isStreaming, updatedAt = now))

                else -> Unit
            }
        }
    }

    override suspend fun addToolCall(
        chatId: ChatId,
        runId: AgentRunId,
        toolCallId: String?,
        name: String,
        arguments: String?,
    ): String {
        val id = idGenerator.newId()
        val now = clock.now()
        store.transaction {
            store.putEntry(
                ChatEntry.ToolCall(
                    id = id,
                    chatId = chatId,
                    runId = runId,
                    sequence = store.nextSequence(chatId),
                    messageSequence = null,
                    createdAt = now,
                    updatedAt = now,
                    toolCallId = toolCallId,
                    name = name,
                    arguments = arguments,
                    status = ToolCallStatus.Pending,
                )
            )
        }
        return id
    }

    override suspend fun updateToolCallStatus(entryId: String, status: ToolCallStatus) {
        val now = clock.now()
        store.transaction {
            val entry = store.findEntry(entryId) as? ChatEntry.ToolCall ?: return@transaction
            store.putEntry(entry.copy(status = status, updatedAt = now))
        }
    }

    override suspend fun findToolCallEntryId(chatId: ChatId, toolCallId: String): String? =
        store.transaction {
            store.entries.value[chatId]
                .orEmpty()
                .filterIsInstance<ChatEntry.ToolCall>()
                .lastOrNull { it.toolCallId == toolCallId }
                ?.id
        }

    override suspend fun settleStreamingEntries(runId: AgentRunId) {
        val now = clock.now()
        store.transaction {
            store.entries.value.values.flatten().forEach { entry ->
                when {
                    entry.runId != runId -> Unit

                    entry is ChatEntry.AssistantMessage && entry.isStreaming ->
                        store.putEntry(entry.copy(isStreaming = false, updatedAt = now))

                    entry is ChatEntry.Reasoning && entry.isStreaming ->
                        store.putEntry(entry.copy(isStreaming = false, updatedAt = now))

                    else -> Unit
                }
            }
        }
    }
}
