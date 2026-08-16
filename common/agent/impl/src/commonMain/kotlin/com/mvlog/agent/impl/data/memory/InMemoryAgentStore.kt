package com.mvlog.agent.impl.data.memory

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.AgentRun
import com.mvlog.agent.impl.domain.entity.ChatEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-lifetime backing store for the agent subsystem.
 *
 * Exists so the whole pipeline — api, use cases, coordinator, run execution, UI — can be built and
 * exercised before database creation is solved. The Room-backed repositories implement the same
 * interfaces and swap in without touching anything above them. The one behaviour this cannot
 * provide is survival across process death.
 *
 * [chats] and [runs] are a single shared state so that operations spanning both (accepting a
 * prompt) can be made atomic under [mutex], mirroring the `@Transaction` the Room implementation
 * uses.
 */
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
