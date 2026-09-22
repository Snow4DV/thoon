package com.mvlog.thoon.database

import com.mvlog.agent.impl.data.room.dao.AgentConfigDao
import com.mvlog.agent.impl.data.room.dao.AgentCheckpointDao
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.agent.impl.data.room.dao.ToolApprovalRuleDao
import com.mvlog.database.dao.DaoRegistry
import com.mvlog.database.dao.ThoonDao
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

class DatabaseRegistrationTest {

    @Test
    fun everyDaoIsRegistered() {
        val registered = mutableSetOf<KClass<*>>()
        val recorder = object : DaoRegistry {
            override fun <T : ThoonDao> register(daoClass: KClass<T>, provider: () -> T) {
                registered += daoClass
            }
        }

        // The throwing accessor asserts that registering never opens the database.
        DatabaseInitializer().registerDaos(recorder) {
            fail("registering DAOs must not build the database")
        }

        val missing = ALL_DAOS - registered
        if (missing.isNotEmpty()) {
            fail("DAOs not registered in DatabaseInitializer: ${missing.map { it.simpleName }}")
        }
        assertTrue(registered.containsAll(ALL_DAOS))
    }

    private companion object {
        val ALL_DAOS: Set<KClass<*>> = setOf(
            ChatDao::class,
            AgentCheckpointDao::class,
            AgentConfigDao::class,
            ToolApprovalRuleDao::class,
        )
    }
}
