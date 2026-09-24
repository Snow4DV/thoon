package com.mvlog.agent.impl.data.room.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** sessionId is the chat id: the framework keys checkpoints by the session passed to run(). */
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
