package com.mvlog.agent.impl.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
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

    /**
     * Newest conversation first, not most recently written.
     *
     * `lastMessageAt` rather than `updatedAt`, so choosing a different model for a chat does not
     * move it to the top of the list.
     */
    @Query("SELECT * FROM agent_chat ORDER BY coalesce(lastMessageAt, createdAt) DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Query("DELETE FROM agent_chat WHERE id = :chatId")
    suspend fun delete(chatId: String)

    /**
     * Removes a chat and everything behind it.
     *
     * Messages and parts cascade through their foreign keys. `agent_checkpoint` does not — it
     * references a chat by `sessionId` with no key at all — so it is still deleted by hand.
     */
    @Transaction
    suspend fun deleteChat(chatId: String) {
        deleteCheckpoints(chatId)
        delete(chatId)
    }

    @Query("UPDATE agent_chat SET configId = :configId, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun setConfigId(chatId: String, configId: String?, updatedAt: Long)

    @Query("UPDATE agent_chat SET configId = NULL WHERE configId = :configId")
    suspend fun clearConfigOverrides(configId: String)

    @Query("UPDATE agent_chat SET title = :title, updatedAt = :updatedAt WHERE id = :chatId")
    suspend fun setTitle(chatId: String, title: String?, updatedAt: Long)

    // --- conversation ---

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

    /** The turn a diff has to agree with before it may append rather than rewrite. */
    @Query("SELECT * FROM agent_chat_message WHERE chatId = :chatId ORDER BY sequence DESC LIMIT 1")
    suspend fun lastMessage(chatId: String): ChatMessageEntity?

    @Insert
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Insert
    suspend fun insertParts(parts: List<ChatPartEntity>)

    /** Parts cascade with their message. */
    @Query("DELETE FROM agent_chat_message WHERE chatId = :chatId")
    suspend fun deleteMessages(chatId: String)

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

    /**
     * Messages that match, newest chat first, with the chat each belongs to.
     *
     * One row per matching *message*, not per chat: a result that cannot say which message it means
     * cannot open at it. `GROUP BY m.id` collapses a turn that matched in two of its text parts.
     *
     * Three clauses carry the correctness of this, and none is optional:
     *
     * - `kind = 'text'` excludes reasoning and tool output. Measured on a real conversation, 40% of
     *   it is the model thinking privately and 12% is tool traffic — searching that matches chats
     *   where nobody ever said the word.
     * - `role IN ('user','assistant')` excludes the system prompt, which is a text part like any
     *   other. Without it every chat matches any word in the agent's instructions.
     * - `ESCAPE` makes `%` and `_` searchable as themselves. Without it a query of `50%` matches
     *   every message in the database.
     *
     * Matching is on `textLower` against an already-lowercased query, because SQLite folds case for
     * ASCII only.
     *
     * `matchedText` and `messageSequence` are bare columns beside `min(p.sequence)`. SQLite defines
     * those to come from the row that produced the minimum, so the pair is the chat's first matching
     * part rather than an arbitrary one — which is why there is exactly one aggregate here, and why
     * adding a second would quietly make them meaningless.
     *
     * Ordering is by the chat's own recency and then by position within it, deliberately **not** by
     * `m.createdAt`: that column is written as the commit time of a whole batch, so every message a
     * run wrote shares one instant and a rewrite restamps the entire conversation. Ordering by it
     * would be arbitrary within a chat. This ordering also matches `observeAll`, so the search list
     * and the plain list agree, and it keeps a chat's several matches adjacent.
     */
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

    /**
     * Commits a turn: the conversation becomes durable and the journal behind it is dropped.
     *
     * One transaction, because these two facts must never disagree — a committed conversation with
     * its checkpoints still present would replay work that already landed. The row summary is
     * written here too, so the list can never describe a turn the conversation does not contain.
     *
     * [fromSequence] is where the caller's diff decided the incoming turns start: the end of what is
     * already stored when appending, or 0 when the incoming conversation no longer extends it.
     */
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
