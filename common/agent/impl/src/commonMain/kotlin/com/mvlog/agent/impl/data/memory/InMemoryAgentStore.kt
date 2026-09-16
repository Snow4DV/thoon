package com.mvlog.agent.impl.data.memory

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.AgentRun
import com.mvlog.agent.impl.domain.entity.ChatEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Process-lifetime projection of timelines and the run queue; rebuilt from storage on hydrate. */
internal class InMemoryAgentStore {

    val mutex = Mutex()

    val entries = MutableStateFlow<Map<ChatId, List<ChatEntry>>>(emptyMap())

    val runs = MutableStateFlow<Map<AgentRunId, AgentRun>>(emptyMap())

    private var sequences = mutableMapOf<ChatId, Long>()

    /** Must be called under [mutex]. */
    fun nextSequence(chatId: ChatId): Long {
        val next = (sequences[chatId] ?: 0L) + 1L
        sequences[chatId] = next
        return next
    }

    /** Must be called under [mutex]. */
    fun putEntry(entry: ChatEntry) {
        entries.value = entries.value.toMutableMap().apply {
            val existing = get(entry.chatId).orEmpty()
            val index = existing.indexOfFirst { it.id == entry.id }
            put(
                entry.chatId,
                if (index >= 0) {
                    existing.toMutableList().apply { set(index, entry) }
                } else {
                    existing + entry
                }.sortedBy { it.sequence },
            )
        }
    }

    /** Must be called under [mutex]. */
    fun replaceEntries(chatId: ChatId, replacement: List<ChatEntry>) {
        entries.value = entries.value + (chatId to replacement.sortedBy { it.sequence })
        sequences[chatId] = replacement.maxOfOrNull { it.sequence } ?: 0L
    }

    /** Must be called under [mutex]. */
    fun findEntry(entryId: String): ChatEntry? =
        entries.value.values.firstNotNullOfOrNull { chatEntries ->
            chatEntries.firstOrNull { it.id == entryId }
        }

    /** Must be called under [mutex]. */
    fun putRun(run: AgentRun) {
        runs.value = runs.value + (run.id to run)
    }

    suspend fun <T> transaction(block: () -> T): T = mutex.withLock { block() }
}
