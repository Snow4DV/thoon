package com.mvlog.database

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteDriver
import kotlin.coroutines.CoroutineContext

expect class DatabaseBuilderFactory {

    fun <T : RoomDatabase> builder(name: String, initializer: () -> T): RoomDatabase.Builder<T>
}

expect val databaseDispatcher: CoroutineContext

internal expect fun platformSQLiteDriver(): SQLiteDriver

fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyThoonDefaults(
    migrations: List<Migration> = emptyList(),
): RoomDatabase.Builder<T> = this
    .setDriver(platformSQLiteDriver())
    .setQueryCoroutineContext(databaseDispatcher)
    .apply { if (migrations.isNotEmpty()) addMigrations(*migrations.toTypedArray()) }

object ThoonDatabaseNames {

    const val MAIN = "thoon.db"
}
