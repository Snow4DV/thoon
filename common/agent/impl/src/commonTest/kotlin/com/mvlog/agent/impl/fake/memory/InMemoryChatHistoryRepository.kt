package com.mvlog.agent.impl.fake.memory

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class InMemoryChatHistoryRepository : ChatHistoryRepository {

    private val mutex = Mutex()

    private val histories = mutableMapOf<ChatId, ChatHistoryRepository.StoredHistory>()

    override suspend fun load(chatId: ChatId): ChatHistoryRepository.StoredHistory? =
        mutex.withLock { histories[chatId] }

    override suspend fun commit(chatId: ChatId, formatVersion: Int, payload: String) {
        mutex.withLock {
            histories[chatId] = ChatHistoryRepository.StoredHistory(formatVersion, payload)
        }
    }

    override suspend fun clear(chatId: ChatId) {
        mutex.withLock { histories.remove(chatId) }
    }
}
