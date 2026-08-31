package com.mvlog.agent.impl.fake.memory

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.StoredMessage
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Holds whole conversations rather than diffing them.
 *
 * The Room implementation appends only what is new and falls back to a rewrite when the incoming
 * conversation no longer extends what it holds; that logic has its own tests. Repeating it here
 * would mean these tests passing against a second implementation of the thing under test.
 */
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
