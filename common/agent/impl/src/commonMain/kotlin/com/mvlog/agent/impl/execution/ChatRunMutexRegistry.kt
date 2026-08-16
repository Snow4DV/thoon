package com.mvlog.agent.impl.execution

import com.mvlog.agent.api.model.ChatId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serialises runs within a chat while letting different chats run concurrently.
 *
 * Required because conversation history is loaded at the start of a run and written back at its
 * end: two runs overlapping on the same chat would each save a snapshot that ignores the other,
 * and the later write would silently discard the earlier run's turns.
 */
internal class ChatRunMutexRegistry {

    private val guard = Mutex()

    private val mutexes = mutableMapOf<ChatId, Mutex>()

    suspend fun <T> withChatLock(chatId: ChatId, block: suspend () -> T): T =
        mutexFor(chatId).withLock { block() }

    private suspend fun mutexFor(chatId: ChatId): Mutex =
        guard.withLock { mutexes.getOrPut(chatId) { Mutex() } }
}
