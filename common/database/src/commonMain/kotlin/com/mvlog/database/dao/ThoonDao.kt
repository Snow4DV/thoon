package com.mvlog.database.dao

/**
 * Marker for a DAO that can be resolved through [DaoFactory].
 *
 * Every `@Dao` interface in the app implements this. It carries no members — its only job is to
 * keep [DaoFactory.get] from being callable with arbitrary types.
 */
interface ThoonDao
