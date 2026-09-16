package com.mvlog.database.di

import com.mvlog.database.dao.DaoFactory
import com.mvlog.database.dao.MutableDaoFactory
import com.mvlog.di.ApiComponentHolder

interface ThoonDatabaseComponent {

    fun daoFactory(): DaoFactory
}

object ThoonDatabaseComponentHolder : ApiComponentHolder<ThoonDatabaseComponent>()

class DefaultDatabaseComponent(
    private val factory: MutableDaoFactory = MutableDaoFactory(),
) : ThoonDatabaseComponent {

    override fun daoFactory(): DaoFactory = factory

    fun register(block: MutableDaoFactory.() -> Unit): DefaultDatabaseComponent =
        apply { factory.block() }
}
