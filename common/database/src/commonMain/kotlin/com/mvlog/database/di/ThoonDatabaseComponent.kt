package com.mvlog.database.di

import com.mvlog.database.dao.DaoFactory
import com.mvlog.database.dao.MutableDaoFactory
import com.mvlog.di.ApiComponentHolder

/**
 * What the database subsystem exposes to the rest of the app.
 *
 * Only DAOs cross this boundary — never the database itself, so no consumer can open a
 * transaction, close the connection, or depend on the concrete database type.
 */
interface ThoonDatabaseComponent {

    fun daoFactory(): DaoFactory
}

object ThoonDatabaseComponentHolder : ApiComponentHolder<ThoonDatabaseComponent>()

/**
 * Ready-made component for whichever module creates the database: build it, register each DAO
 * against it, and hand the result to [ThoonDatabaseComponentHolder].
 */
class DefaultDatabaseComponent(
    private val factory: MutableDaoFactory = MutableDaoFactory(),
) : ThoonDatabaseComponent {

    override fun daoFactory(): DaoFactory = factory

    fun register(block: MutableDaoFactory.() -> Unit): DefaultDatabaseComponent =
        apply { factory.block() }
}
