package com.mvlog.thoon.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.mvlog.agent.impl.data.room.dao.AgentConfigDao
import com.mvlog.agent.impl.data.room.dao.AgentCheckpointDao
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.agent.impl.data.room.entity.AgentConfigEntity
import com.mvlog.agent.impl.data.room.entity.AgentCheckpointEntity
import com.mvlog.agent.impl.data.room.entity.ChatEntity
import com.mvlog.agent.impl.data.room.entity.ChatMessageEntity
import com.mvlog.agent.impl.data.room.entity.ChatPartEntity
import com.mvlog.agent.impl.data.room.entity.AgentSettingsEntity

@Database(
    entities = [
        ChatEntity::class,
        ChatMessageEntity::class,
        ChatPartEntity::class,
        AgentCheckpointEntity::class,
        AgentConfigEntity::class,
        AgentSettingsEntity::class,
    ],
    version = ThoonDatabase.VERSION,
    exportSchema = true,
)
@ConstructedBy(ThoonDatabaseConstructor::class)
abstract class ThoonDatabase : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    abstract fun agentCheckpointDao(): AgentCheckpointDao

    abstract fun agentConfigDao(): AgentConfigDao

    companion object {
        const val VERSION = 2
    }
}

@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object ThoonDatabaseConstructor : RoomDatabaseConstructor<ThoonDatabase> {
    override fun initialize(): ThoonDatabase
}
