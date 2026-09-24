package com.mvlog.agent.impl.data.room.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
