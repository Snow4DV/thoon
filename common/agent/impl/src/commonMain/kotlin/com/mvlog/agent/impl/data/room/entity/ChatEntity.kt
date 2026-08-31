package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One durable row per chat: what the list needs, and nothing else.
 *
 * The conversation itself lives in `agent_chat_message` / `agent_chat_part`. It used to be a JSON
 * blob on this row, which meant every metadata read — the chats list included — loaded whole
 * conversations to render a title and a timestamp.
 */
@Entity(
    tableName = "agent_chat",
    indices = [Index("configId"), Index("lastMessageAt")],
)
data class ChatEntity(
    @PrimaryKey
    val id: String,
    /** Per-chat configuration override; null means follow the app-wide default. */
    val configId: String?,
    val title: String?,
    /**
     * When someone last said something, as opposed to when the row last changed.
     *
     * Distinct from [updatedAt] deliberately: that bumps on any write, so choosing a different model
     * for a chat would otherwise move it to the top of the list. Null until the first turn commits.
     */
    val lastMessageAt: Long?,
    /** The last thing said, so the list needs no join. Null until the first turn commits. */
    val lastMessagePreview: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
