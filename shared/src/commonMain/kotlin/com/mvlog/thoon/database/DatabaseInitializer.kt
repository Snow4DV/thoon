package com.mvlog.thoon.database

import com.mvlog.agent.impl.data.room.dao.AgentConfigDao
import com.mvlog.agent.impl.data.room.dao.AgentCheckpointDao
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.database.DatabaseBuilderFactory
import com.mvlog.database.ThoonDatabaseNames
import com.mvlog.database.applyThoonDefaults
import com.mvlog.database.dao.DaoRegistry
import com.mvlog.database.di.DefaultDatabaseComponent
import com.mvlog.database.di.ThoonDatabaseComponentHolder
import com.mvlog.init.BaseInitializer

/**
 * Publishes the database and every feature's DAOs.
 *
 * Nothing here opens the database: the component is built on first access, and each DAO is
 * registered as a lambda that is only invoked when that DAO is first requested.
 *
 * Every DAO a feature declares must be registered here — a missing entry surfaces the first time
 * that feature touches storage, which [ThoonDatabaseTest] guards against.
 */
class DatabaseInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        ThoonDatabaseComponentHolder.set {
            val database by lazy {
                DatabaseBuilderFactory()
                    .builder(ThoonDatabaseNames.MAIN, ThoonDatabaseConstructor::initialize)
                    .applyThoonDefaults(ThoonMigrations.ALL)
                    .build()
            }

            DefaultDatabaseComponent().register { registerDaos(this) { database } }
        }
    }

    /**
     * Separated from [init] so a test can observe which DAOs get registered without building a
     * database. [database] is only dereferenced when a DAO is actually resolved.
     */
    internal fun registerDaos(registry: DaoRegistry, database: () -> ThoonDatabase) {
        registry.register(ChatDao::class) { database().chatDao() }
        registry.register(AgentCheckpointDao::class) { database().agentCheckpointDao() }
        registry.register(AgentConfigDao::class) { database().agentConfigDao() }
    }

    private companion object {
        const val TAG = "ThoonDatabase"
    }
}
