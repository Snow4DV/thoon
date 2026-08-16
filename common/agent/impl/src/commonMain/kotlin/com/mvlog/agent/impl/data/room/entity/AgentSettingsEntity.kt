package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row table holding agent-wide selections.
 *
 * The default configuration lives here rather than as an `isDefault` flag on `agent_config`, which
 * removes the "two rows both marked default" state entirely — there is only ever one value.
 */
@Entity(tableName = "agent_settings")
data class AgentSettingsEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    /** Null when no default is set, including after the default configuration is deleted. */
    val defaultConfigId: String?,
) {
    companion object {
        const val SINGLETON_ID = 0
    }
}
