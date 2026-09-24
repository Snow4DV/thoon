package com.mvlog.agent.impl.data.room.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "agent_settings")
data class AgentSettingsEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val defaultConfigId: String?,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
