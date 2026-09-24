package com.mvlog.agent.impl.data.room.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "agent_chat",
    indices = [Index("configId"), Index("lastMessageAt")],
)
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val configId: String?,
    val title: String?,
    /** Commit time of the last turn, not updatedAt: a config change must not reorder the list. */
    val lastMessageAt: Long?,
    val lastMessagePreview: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
