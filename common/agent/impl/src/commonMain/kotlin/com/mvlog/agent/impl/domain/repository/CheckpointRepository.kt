package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId

internal interface CheckpointRepository {

    suspend fun latest(chatId: ChatId): StoredCheckpoint?

    suspend fun byId(chatId: ChatId, checkpointId: String): StoredCheckpoint?

    suspend fun all(chatId: ChatId): List<StoredCheckpoint>

    suspend fun save(chatId: ChatId, checkpoint: StoredCheckpoint)

    suspend fun chatsWithCheckpoints(): List<ChatId>

    suspend fun clear(chatId: ChatId)

    data class StoredCheckpoint(
        val checkpointId: String,
        val createdAt: Long,
        val version: Long,
        val payload: String,
    )
}
