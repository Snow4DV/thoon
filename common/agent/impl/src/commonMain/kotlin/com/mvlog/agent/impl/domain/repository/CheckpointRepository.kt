package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId

/**
 * Snapshots of runs that have not committed.
 *
 * Written continuously while a run executes and dropped when it commits, so what remains is work
 * that never finished — the record recovery reads to show a partial run and to give the model
 * context for what already happened.
 *
 * Payload is opaque here for the same reason as in [ChatHistoryRepository].
 */
internal interface CheckpointRepository {

    suspend fun latest(chatId: ChatId): StoredCheckpoint?

    suspend fun byId(chatId: ChatId, checkpointId: String): StoredCheckpoint?

    suspend fun all(chatId: ChatId): List<StoredCheckpoint>

    suspend fun save(chatId: ChatId, checkpoint: StoredCheckpoint)

    /** Chats holding uncommitted work — the starting point for recovery. */
    suspend fun chatsWithCheckpoints(): List<ChatId>

    suspend fun clear(chatId: ChatId)

    data class StoredCheckpoint(
        val checkpointId: String,
        val createdAt: Long,
        val version: Long,
        val payload: String,
    )
}
