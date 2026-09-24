package com.mvlog.thoon.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.mvlog.agent.impl.data.room.dao.AgentConfigDao
import com.mvlog.agent.impl.data.room.dao.AgentCheckpointDao
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.agent.impl.data.room.dao.ToolApprovalRuleDao
import com.mvlog.agent.impl.data.room.entity.AgentConfigEntity
import com.mvlog.agent.impl.data.room.entity.AgentCheckpointEntity
import com.mvlog.agent.impl.data.room.entity.ChatEntity
import com.mvlog.agent.impl.data.room.entity.ChatMessageEntity
import com.mvlog.agent.impl.data.room.entity.ChatPartEntity
import com.mvlog.agent.impl.data.room.entity.AgentSettingsEntity
import com.mvlog.agent.impl.data.room.entity.ToolApprovalRuleEntity

@Database(
    entities = [
        ChatEntity::class,
        ChatMessageEntity::class,
        ChatPartEntity::class,
        AgentCheckpointEntity::class,
        AgentConfigEntity::class,
        AgentSettingsEntity::class,
        ToolApprovalRuleEntity::class,
    ],
    version = ThoonDatabase.VERSION,
    exportSchema = true,
)
@ConstructedBy(ThoonDatabaseConstructor::class)
abstract class ThoonDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    abstract fun agentCheckpointDao(): AgentCheckpointDao

    abstract fun agentConfigDao(): AgentConfigDao

    abstract fun toolApprovalRuleDao(): ToolApprovalRuleDao

    companion object {
        const val VERSION = 3
    }
}

@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object ThoonDatabaseConstructor : RoomDatabaseConstructor<ThoonDatabase> {
    override fun initialize(): ThoonDatabase
}
