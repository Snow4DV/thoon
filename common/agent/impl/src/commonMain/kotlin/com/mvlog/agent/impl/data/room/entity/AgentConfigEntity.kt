package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agent_config",
    indices = [Index("kind")],
)
data class AgentConfigEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val kind: String,
    val modelId: String,
    val payloadJson: String,
)
