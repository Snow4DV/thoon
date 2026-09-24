package com.mvlog.agent.impl.data.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "agent_chat_message",
    indices = [Index("chatId", "sequence")],
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val sequence: Long,
    val role: String,
    val metaInfoJson: String,
    val createdAt: Long,
)
