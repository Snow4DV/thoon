package com.mvlog.database.dao

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

internal class DefaultDaoFactory : MutableDaoFactory {

    private val lock = SynchronizedObject()

    private val providers = mutableMapOf<KClass<*>, () -> ThoonDao>()

    private val instances = mutableMapOf<KClass<*>, ThoonDao>()

    override fun <T : ThoonDao> register(daoClass: KClass<T>, provider: () -> T) {
        synchronized(lock) { providers[daoClass] = provider }
    }

    // Sound: register() is the only writer and pairs KClass<T> with () -> T.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ThoonDao> get(daoClass: KClass<T>): T = synchronized(lock) {
        instances.getOrPut(daoClass) {
            val provider = providers[daoClass]
                ?: error(
                    "No DAO registered for ${daoClass.simpleName}. " +
                        "Register it where the database is created."
                )
            provider()
        } as T
    }
}
