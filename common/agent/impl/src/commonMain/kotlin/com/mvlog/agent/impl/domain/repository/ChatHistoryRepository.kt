package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.StoredMessage

/**
 * The committed conversation of a chat, as rows.
 *
 * Speaks [StoredMessage] rather than agent-framework messages: only the codec knows what those are,
 * so that dependency stays in one corner of the module instead of spreading through storage.
 */
internal interface ChatHistoryRepository {

    suspend fun load(chatId: ChatId): List<StoredMessage>

    /**
     * Commits [messages] as the chat's conversation and discards the checkpoints behind it,
     * atomically.
     *
     * Takes the whole conversation because that is what the agent framework hands over on every
     * turn; deciding how much of it is actually new is this repository's job, not the caller's.
     */
    suspend fun commit(chatId: ChatId, messages: List<StoredMessage>)

    suspend fun clear(chatId: ChatId)
}
