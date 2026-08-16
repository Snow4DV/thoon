package com.mvlog.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext

/**
 * Creates a Room builder for the current platform.
 *
 * A class rather than a function because the platform halves differ in what they need — a Context
 * on Android, a file path elsewhere — and because Room's generic builder overloads would otherwise
 * require a reified type parameter, which an `expect` function cannot have. Passing the database's
 * generated initializer explicitly avoids that entirely.
 */
expect class DatabaseBuilderFactory {

    fun <T : RoomDatabase> builder(name: String, initializer: () -> T): RoomDatabase.Builder<T>
}

/** The dispatcher Room runs queries on. */
expect val databaseDispatcher: CoroutineContext

/**
 * Applies the configuration every Thoon database shares.
 *
 * Notably absent: `fallbackToDestructiveMigration`. The app keeps all features in one file, so a
 * destructive fallback would discard unrelated features' data to recover from one schema mistake.
 * Missing migrations should fail loudly instead.
 */
fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyThoonDefaults(
    migrations: List<Migration> = emptyList(),
): RoomDatabase.Builder<T> = this
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(databaseDispatcher)
    .apply { if (migrations.isNotEmpty()) addMigrations(*migrations.toTypedArray()) }

object ThoonDatabaseNames {

    /** The single database file backing every feature. */
    const val MAIN = "thoon.db"
}
