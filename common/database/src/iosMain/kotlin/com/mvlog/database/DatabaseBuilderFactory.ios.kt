package com.mvlog.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.coroutines.CoroutineContext

actual class DatabaseBuilderFactory {

    actual fun <T : RoomDatabase> builder(
        name: String,
        initializer: () -> T,
    ): RoomDatabase.Builder<T> {
        // Sound: the initializer is always passed, so Room never takes its reified reflective path.
        @Suppress("UNCHECKED_CAST")
        return Room.databaseBuilder<RoomDatabase>(
            name = "${documentsDirectory()}/$name",
            factory = { initializer() },
        ) as RoomDatabase.Builder<T>
    }

    // Documents, not Caches: iOS may evict Caches under storage pressure.
    @OptIn(ExperimentalForeignApi::class)
    private fun documentsDirectory(): String {
        val url: NSURL = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        ) ?: error("Unable to resolve the iOS documents directory for the database")

        return requireNotNull(url.path) { "Documents directory URL has no path" }
    }
}

// Default is enough: Room serialises writes and the bundled driver locks; queries only need to
// stay off main.
actual val databaseDispatcher: CoroutineContext = Dispatchers.Default

internal actual fun platformSQLiteDriver(): SQLiteDriver = BundledSQLiteDriver()
