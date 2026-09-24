package com.mvlog.agent.impl.data.room.entity

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "agent_chat_part",
    indices = [Index("messageId", "sequence"), Index("kind")],
    foreignKeys = [
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ChatPartEntity(
    @PrimaryKey
    val id: String,
    val messageId: String,
    /** Reasoning precedes the answer it produced; order is meaning. */
    val sequence: Long,
    val kind: String,
    val text: String?,
    val textLower: String?,
    /** Set for tool calls only; nothing queries it yet. */
    val toolName: String?,
    val payloadJson: String,
)
