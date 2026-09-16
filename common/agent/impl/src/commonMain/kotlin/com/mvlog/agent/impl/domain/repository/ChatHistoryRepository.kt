package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.StoredMessage

internal interface ChatHistoryRepository {

    suspend fun load(chatId: ChatId): List<StoredMessage>

    suspend fun commit(chatId: ChatId, messages: List<StoredMessage>)

    suspend fun clear(chatId: ChatId)
}
