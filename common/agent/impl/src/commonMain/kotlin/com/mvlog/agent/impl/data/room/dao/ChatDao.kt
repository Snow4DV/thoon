package com.mvlog.agent.impl.data.room.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.mvlog.agent.impl.data.room.entity.ChatEntity
import com.mvlog.agent.impl.data.room.entity.ChatMessageEntity
import com.mvlog.agent.impl.data.room.entity.ChatPartEntity
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

    @Query("SELECT * FROM agent_chat ORDER BY coalesce(lastMessageAt, createdAt) DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("DELETE FROM agent_chat WHERE id = :chatId")
    suspend fun delete(chatId: String)

    @Transaction
    suspend fun deleteChat(chatId: String) {
        // agent_checkpoint has no FK to agent_chat, so it does not cascade.
        deleteCheckpoints(chatId)
        delete(chatId)
    }

    @Query("UPDATE agent_chat SET configId = :configId, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun setConfigId(chatId: String, configId: String?, updatedAt: Long)

    @Query("UPDATE agent_chat SET configId = NULL WHERE configId = :configId")
    suspend fun clearConfigOverrides(configId: String)

    @Query("UPDATE agent_chat SET title = :title, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun setTitle(chatId: String, title: String?, updatedAt: Long)

    @Query("SELECT * FROM agent_chat_message WHERE chatId = :chatId ORDER BY sequence ASC")
    suspend fun messages(chatId: String): List<ChatMessageEntity>

    @Query(
        """
        SELECT p.* FROM agent_chat_part p
        JOIN agent_chat_message m ON m.id = p.messageId
        WHERE m.chatId = :chatId
        ORDER BY m.sequence ASC, p.sequence ASC
        """
    )
    suspend fun parts(chatId: String): List<ChatPartEntity>

    @Query("SELECT count(*) FROM agent_chat_message WHERE chatId = :chatId")
    suspend fun messageCount(chatId: String): Int

    @Query("SELECT * FROM agent_chat_message WHERE chatId = :chatId ORDER BY sequence DESC LIMIT 1")
    suspend fun lastMessage(chatId: String): ChatMessageEntity?

    @Insert
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Insert
    suspend fun insertParts(parts: List<ChatPartEntity>)

    @Query("DELETE FROM agent_chat_message WHERE chatId = :chatId AND sequence >= :fromSequence")
    suspend fun deleteMessagesFrom(chatId: String, fromSequence: Long)

    @Query(
        """
        UPDATE agent_chat
        SET lastMessageAt = :lastMessageAt, lastMessagePreview = :preview, updatedAt = :updatedAt
        WHERE id = :chatId
        """
    )
    suspend fun setLastMessage(
        chatId: String,
        lastMessageAt: Long?,
        preview: String?,
        updatedAt: Long,
    )

    // GROUP BY m.id: one row per matching turn, even when two of its text parts matched.
    // matchedText/messageSequence are bare columns beside the one min(): SQLite takes them from the
    // min row. A second aggregate would make them arbitrary.
    // Same order as observeAll, then position in chat, so a chat's matches sit together.
    @Query(
        """
        SELECT c.*,
               m.sequence AS messageSequence,
               p.text AS matchedText,
               min(p.sequence) AS matchedPartSequence
        FROM agent_chat c
        JOIN agent_chat_message m ON m.chatId = c.id
        JOIN agent_chat_part p ON p.messageId = m.id
        WHERE p.kind = 'text'
          AND m.role IN ('user', 'assistant')
          AND p.textLower LIKE '%' || :query || '%' ESCAPE '\'
        GROUP BY m.id
        ORDER BY coalesce(c.lastMessageAt, c.createdAt) DESC, c.id, m.sequence ASC
        LIMIT :limit
        """
    )
    fun search(query: String, limit: Int): Flow<List<ChatSearchRow>>

    @Query("DELETE FROM agent_checkpoint WHERE sessionId = :chatId")
    suspend fun deleteCheckpoints(chatId: String)

    // Summary columns land in the same transaction, so the list never describes a turn the
    // conversation lacks.
    @Transaction
    suspend fun commitConversation(
        chatId: String,
        fromSequence: Long,
        messages: List<ChatMessageEntity>,
        parts: List<ChatPartEntity>,
        lastMessageAt: Long?,
        preview: String?,
        updatedAt: Long,
    ) {
        deleteMessagesFrom(chatId, fromSequence)
        insertMessages(messages)
        insertParts(parts)
        setLastMessage(
            chatId = chatId,
            lastMessageAt = lastMessageAt,
            preview = preview,
            updatedAt = updatedAt,
        )
        deleteCheckpoints(chatId)
    }
}
