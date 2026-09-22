package com.mvlog.agent.impl.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mvlog.agent.impl.data.room.entity.ToolApprovalRuleEntity
import com.mvlog.database.dao.ThoonDao
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolApprovalRuleDao : ThoonDao {

    @Upsert
    suspend fun upsert(rule: ToolApprovalRuleEntity)

    @Query("SELECT * FROM agent_tool_approval_rule WHERE chatId IS NULL ORDER BY createdAt")
    fun observeGlobal(): Flow<List<ToolApprovalRuleEntity>>

    @Query("SELECT * FROM agent_tool_approval_rule WHERE chatId = :chatId ORDER BY createdAt")
    fun observeForChat(chatId: String): Flow<List<ToolApprovalRuleEntity>>

    @Query(
        """
        SELECT * FROM agent_tool_approval_rule
        WHERE chatId IS NULL OR chatId = :chatId
        ORDER BY createdAt
        """
    )
    fun observeApplicable(chatId: String): Flow<List<ToolApprovalRuleEntity>>

    @Query(
        """
        SELECT * FROM agent_tool_approval_rule
        WHERE chatId IS NULL OR chatId = :chatId
        ORDER BY createdAt
        """
    )
    suspend fun applicable(chatId: String): List<ToolApprovalRuleEntity>

    @Query("DELETE FROM agent_tool_approval_rule WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM agent_tool_approval_rule WHERE chatId = :chatId")
    suspend fun deleteForChat(chatId: String)
}
