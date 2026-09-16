package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
