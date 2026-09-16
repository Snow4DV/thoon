package com.mvlog.database.dao

import kotlin.reflect.KClass

interface DaoFactory {

    fun <T : ThoonDao> get(daoClass: KClass<T>): T
}

inline fun <reified T : ThoonDao> DaoFactory.get(): T = get(T::class)

interface DaoRegistry {

    fun <T : ThoonDao> register(daoClass: KClass<T>, provider: () -> T)
}

interface MutableDaoFactory : DaoFactory, DaoRegistry

fun MutableDaoFactory(): MutableDaoFactory = DefaultDaoFactory()
