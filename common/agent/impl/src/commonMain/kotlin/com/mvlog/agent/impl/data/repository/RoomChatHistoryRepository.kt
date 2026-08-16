package com.mvlog.agent.impl.data.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import com.mvlog.agent.impl.util.AgentClock

internal class RoomChatHistoryRepository(
    private val dao: ChatDao,
    private val clock: AgentClock,
) : ChatHistoryRepository {

    override suspend fun load(chatId: ChatId): ChatHistoryRepository.StoredHistory? {
        val chat = dao.get(chatId.value) ?: return null
        val payload = chat.historyJson ?: return null
        return ChatHistoryRepository.StoredHistory(
            formatVersion = chat.formatVersion,
            payload = payload,
        )
    }

    override suspend fun commit(chatId: ChatId, formatVersion: Int, payload: String) {
        dao.commitHistory(
            chatId = chatId.value,
            historyJson = payload,
            formatVersion = formatVersion,
            updatedAt = clock.now().toEpochMilliseconds(),
        )
    }

    override suspend fun clear(chatId: ChatId) {
        dao.setHistory(
            chatId = chatId.value,
            historyJson = null,
            formatVersion = 0,
            updatedAt = clock.now().toEpochMilliseconds(),
        )
    }
}
