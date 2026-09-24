package com.mvlog.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual class DatabaseBuilderFactory {

    actual fun <T : RoomDatabase> builder(
        name: String,
        initializer: () -> T,
    ): RoomDatabase.Builder<T> {
        val context = AndroidDatabaseContext.require()
        val path = context.getDatabasePath(name).absolutePath

        // Sound: the initializer is always passed, so Room never takes its reified reflective path.
        @Suppress("UNCHECKED_CAST")
        return Room.databaseBuilder<RoomDatabase>(context, path) { initializer() }
            as RoomDatabase.Builder<T>
    }
}

actual val databaseDispatcher: CoroutineContext = Dispatchers.IO

internal actual fun platformSQLiteDriver(): SQLiteDriver = BundledSQLiteDriver()
