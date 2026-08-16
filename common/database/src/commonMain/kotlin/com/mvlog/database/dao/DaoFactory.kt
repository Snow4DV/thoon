package com.mvlog.database.dao

import kotlin.reflect.KClass

/**
 * Hands out DAO instances by type.
 *
 * Lets a feature module reach its own DAOs without depending on the module that owns the database:
 * the database lives downstream (it must see every module's entities), so the instance travels
 * back up through here instead.
 */
interface DaoFactory {

    fun <T : ThoonDao> get(daoClass: KClass<T>): T
}

/** `daoFactory().get<ChatDao>()` */
inline fun <reified T : ThoonDao> DaoFactory.get(): T = get(T::class)

/**
 * Write side of [DaoFactory], used by whichever module actually creates the database.
 *
 * Registration takes a lambda rather than an instance so that declaring what exists does not force
 * the database to be opened.
 */
interface DaoRegistry {

    fun <T : ThoonDao> register(daoClass: KClass<T>, provider: () -> T)
}

interface MutableDaoFactory : DaoFactory, DaoRegistry

fun MutableDaoFactory(): MutableDaoFactory = DefaultDaoFactory()
