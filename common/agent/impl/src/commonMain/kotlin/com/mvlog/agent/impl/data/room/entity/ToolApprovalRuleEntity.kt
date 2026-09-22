package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_tool_approval_rule",
    indices = [Index("toolName"), Index("chatId")],
)
data class ToolApprovalRuleEntity(
    @PrimaryKey
    val id: String,
    /** Null is a global rule. No foreign key: deleteChat removes them by hand, like checkpoints. */
    val chatId: String?,
    val toolName: String,
    val parametersJson: String,
    val createdAt: Long,
)
