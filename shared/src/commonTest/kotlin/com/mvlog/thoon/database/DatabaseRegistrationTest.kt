package com.mvlog.thoon.database

import com.mvlog.agent.impl.data.room.dao.AgentConfigDao
import com.mvlog.agent.impl.data.room.dao.AgentCheckpointDao
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.database.dao.DaoRegistry
import com.mvlog.database.dao.ThoonDao
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Guards the one weakness of resolving DAOs by type: nothing forces [DatabaseInitializer] to
 * register a DAO a feature has just added, so a miss would only surface at runtime, in that
 * feature, on first use. This turns it into a build failure instead.
 *
 * Add every new DAO to [ALL_DAOS] alongside registering it.
 */
class DatabaseRegistrationTest {

    @Test
    fun everyDaoIsRegistered() {
        val registered = mutableSetOf<KClass<*>>()
        val recorder = object : DaoRegistry {
            override fun <T : ThoonDao> register(daoClass: KClass<T>, provider: () -> T) {
                registered += daoClass
            }
        }

        // The throwing accessor doubles as an assertion that registering never opens the database.
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
        )
    }
}
