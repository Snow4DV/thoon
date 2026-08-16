package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId

/**
 * The committed conversation of a chat.
 *
 * Payload is an opaque string on purpose: only the codec knows it holds agent-framework messages, so
 * that dependency stays in one corner of the module rather than spreading through storage.
 */
internal interface ChatHistoryRepository {

    suspend fun load(chatId: ChatId): StoredHistory?

    /**
     * Commits [payload] as the chat's history and discards the checkpoints behind it, atomically.
     *
     * The two halves are one write because a committed history alongside live checkpoints would
     * replay work that already landed.
     */
    suspend fun commit(chatId: ChatId, formatVersion: Int, payload: String)

    suspend fun clear(chatId: ChatId)

    data class StoredHistory(
        val formatVersion: Int,
        val payload: String,
    )
}
