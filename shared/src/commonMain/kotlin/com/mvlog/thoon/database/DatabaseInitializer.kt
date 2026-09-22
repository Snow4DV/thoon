package com.mvlog.thoon.database

import com.mvlog.agent.impl.data.room.dao.AgentConfigDao
import com.mvlog.agent.impl.data.room.dao.AgentCheckpointDao
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.agent.impl.data.room.dao.ToolApprovalRuleDao
import com.mvlog.database.DatabaseBuilderFactory
import com.mvlog.database.ThoonDatabaseNames
import com.mvlog.database.applyThoonDefaults
import com.mvlog.database.dao.DaoRegistry
import com.mvlog.database.di.DefaultDatabaseComponent
import com.mvlog.database.di.ThoonDatabaseComponentHolder
import com.mvlog.init.BaseInitializer

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

    // Apart from init() so DatabaseRegistrationTest can record registrations without opening a
    // database.
    internal fun registerDaos(registry: DaoRegistry, database: () -> ThoonDatabase) {
        registry.register(ChatDao::class) { database().chatDao() }
        registry.register(AgentCheckpointDao::class) { database().agentCheckpointDao() }
        registry.register(AgentConfigDao::class) { database().agentConfigDao() }
        registry.register(ToolApprovalRuleDao::class) { database().toolApprovalRuleDao() }
    }

    private companion object {
        const val TAG = "ThoonDatabase"
    }
}
