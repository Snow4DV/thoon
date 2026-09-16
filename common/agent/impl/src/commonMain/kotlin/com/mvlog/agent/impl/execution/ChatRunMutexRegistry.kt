package com.mvlog.agent.impl.execution

import com.mvlog.agent.api.model.ChatId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ChatRunMutexRegistry {

    private val guard = Mutex()

    private val mutexes = mutableMapOf<ChatId, Mutex>()

    suspend fun <T> withChatLock(chatId: ChatId, block: suspend () -> T): T =
        mutexFor(chatId).withLock { block() }

    private suspend fun mutexFor(chatId: ChatId): Mutex =
        guard.withLock { mutexes.getOrPut(chatId) { Mutex() } }
}
