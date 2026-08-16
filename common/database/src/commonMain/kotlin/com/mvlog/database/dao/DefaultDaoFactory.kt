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

    /**
     * The cast is sound because [register] is the only way in, and it can only accept a
     * `() -> T` for a `KClass<T>` — nothing else can be stored under this key.
     */
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
