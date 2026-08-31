package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One turn by one speaker.
 *
 * A turn is not one piece of content: an assistant turn routinely holds reasoning *and* prose, or
 * reasoning *and* a tool call. Those live in [ChatPartEntity]; this row carries only who spoke, when,
 * and where the turn sits in the conversation.
 *
 * [metaInfoJson] is the framework's own metadata, stored as it serialises it — timestamps today,
 * token counts and finish reasons in some versions. Opaque here on purpose.
 */
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
    /** Position in the conversation. The model is replayed in this order, so it is load-bearing. */
    val sequence: Long,
    /** `system`, `user` or `assistant`. */
    val role: String,
    val metaInfoJson: String,
    val createdAt: Long,
)
