package com.mvlog.agent.impl.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.mvlog.agent.impl.data.room.entity.ChatEntity
import com.mvlog.database.dao.ThoonDao
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao : ThoonDao {

    @Upsert
    suspend fun upsert(chat: ChatEntity)

    @Query("SELECT * FROM agent_chat WHERE id = :chatId")
    suspend fun get(chatId: String): ChatEntity?

    @Query("SELECT * FROM agent_chat WHERE id = :chatId")
    fun observe(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM agent_chat ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("DELETE FROM agent_chat WHERE id = :chatId")
    suspend fun delete(chatId: String)

    @Query("UPDATE agent_chat SET configId = :configId, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun setConfigId(chatId: String, configId: String?, updatedAt: Long)

    @Query("UPDATE agent_chat SET configId = NULL WHERE configId = :configId")
    suspend fun clearConfigOverrides(configId: String)

    @Query("UPDATE agent_chat SET title = :title, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun setTitle(chatId: String, title: String?, updatedAt: Long)

    @Query(
        """
        UPDATE agent_chat
        SET historyJson = :historyJson, formatVersion = :formatVersion, updatedAt = :updatedAt
        WHERE id = :chatId
        """
    )
    suspend fun setHistory(
        chatId: String,
        historyJson: String?,
        formatVersion: Int,
        updatedAt: Long,
    )

    @Query("DELETE FROM agent_checkpoint WHERE sessionId = :chatId")
    suspend fun deleteCheckpoints(chatId: String)

    /**
     * Commits a turn: the conversation becomes durable and the journal behind it is dropped.
     *
     * One transaction, because these two facts must never disagree — a committed history with its
     * checkpoints still present would replay work that already landed.
     */
    @Transaction
    suspend fun commitHistory(
        chatId: String,
        historyJson: String,
        formatVersion: Int,
        updatedAt: Long,
    ) {
        setHistory(
            chatId = chatId,
            historyJson = historyJson,
            formatVersion = formatVersion,
            updatedAt = updatedAt,
        )
        deleteCheckpoints(chatId)
    }
}
