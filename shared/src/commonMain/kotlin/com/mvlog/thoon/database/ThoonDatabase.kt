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
import com.mvlog.agent.impl.data.room.entity.AgentSettingsEntity

/**
 * The app's single database.
 *
 * Every feature's tables live here, which is why this module depends on each module that declares
 * entities. Features never see this type — they resolve their own DAOs through the factory in
 * `common:database`.
 *
 * [VERSION] covers all features at once: any feature's schema change bumps it, and its migration
 * belongs in [ThoonMigrations].
 */
@Database(
    entities = [
        // agent
        ChatEntity::class,
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
        const val VERSION = 1
    }
}

// Actuals are generated per platform by Room's KSP processor.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object ThoonDatabaseConstructor : RoomDatabaseConstructor<ThoonDatabase> {
    override fun initialize(): ThoonDatabase
}
