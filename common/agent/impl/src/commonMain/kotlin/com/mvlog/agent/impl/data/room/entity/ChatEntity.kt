package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One durable row per chat: its committed conversation and the metadata around it.
 *
 * [historyJson] is the only column that ever reaches the agent framework — everything else is ours.
 */
@Entity(
    tableName = "agent_chat",
    indices = [Index("configId"), Index("updatedAt")],
)
data class ChatEntity(
    @PrimaryKey
    val id: String,
    /** Per-chat configuration override; null means follow the app-wide default. */
    val configId: String?,
    val title: String?,
    /**
     * Committed conversation, encoded by `KoogHistoryCodec`. Empty until the first turn commits.
     *
     * "Committed" is literal: a turn lands here only when a run completes and its history is
     * written. Work in progress lives in `agent_checkpoint` until then.
     */
    val historyJson: String?,
    /** Guards against decoding a payload written by an incompatible serialization format. */
    val formatVersion: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
