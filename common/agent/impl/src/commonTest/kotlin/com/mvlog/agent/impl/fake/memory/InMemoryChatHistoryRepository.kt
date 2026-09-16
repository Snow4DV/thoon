package com.mvlog.agent.impl.fake.memory

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.StoredMessage
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Stores the whole conversation; the append/rewrite diff is Room's and has its own tests. */
internal class InMemoryChatHistoryRepository : ChatHistoryRepository {

    private val mutex = Mutex()

    private val conversations = mutableMapOf<ChatId, List<StoredMessage>>()

    override suspend fun load(chatId: ChatId): List<StoredMessage> =
        mutex.withLock { conversations[chatId].orEmpty() }

    override suspend fun commit(chatId: ChatId, messages: List<StoredMessage>) {
        mutex.withLock { conversations[chatId] = messages }
    }

    override suspend fun clear(chatId: ChatId) {
        mutex.withLock { conversations.remove(chatId) }
    }
}
