package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A point-in-time snapshot of a run in progress — the write-ahead journal for a chat.
 *
 * Rows live only until the run commits: writing a chat's history deletes its checkpoints in the same
 * transaction. What survives here is therefore work that never committed, which is exactly what
 * recovery needs to show.
 *
 * [sessionId] is the chat id — the agent framework keys checkpoints by the session passed to `run`.
 */
@Entity(
    tableName = "agent_checkpoint",
    indices = [Index(value = ["sessionId", "createdAt"])],
)
data class AgentCheckpointEntity(
    @PrimaryKey
    val checkpointId: String,
    val sessionId: String,
    val createdAt: Long,
    val version: Long,
    val payloadJson: String,
)
