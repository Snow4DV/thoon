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
        /**
         * No migration exists from 1: conversations moved from a JSON blob into message and part
         * rows while the app was pre-release, and reinstalling was cheaper than carrying migration
         * code nobody would ever exercise again. An older install therefore fails loudly on open,
         * which is what the deliberate absence of `fallbackToDestructiveMigration` is for.
         */
        const val VERSION = 2
    }
}

// Actuals are generated per platform by Room's KSP processor.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object ThoonDatabaseConstructor : RoomDatabaseConstructor<ThoonDatabase> {
    override fun initialize(): ThoonDatabase
}
