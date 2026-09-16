package com.mvlog.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

expect class DatabaseBuilderFactory {

    fun <T : RoomDatabase> builder(name: String, initializer: () -> T): RoomDatabase.Builder<T>
}

expect val databaseDispatcher: CoroutineContext

fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyThoonDefaults(
    migrations: List<Migration> = emptyList(),
): RoomDatabase.Builder<T> = this
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(databaseDispatcher)
    .apply { if (migrations.isNotEmpty()) addMigrations(*migrations.toTypedArray()) }

object ThoonDatabaseNames {

    const val MAIN = "thoon.db"
}
