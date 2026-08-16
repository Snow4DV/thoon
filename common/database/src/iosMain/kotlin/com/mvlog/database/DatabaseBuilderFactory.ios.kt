package com.mvlog.database

import androidx.room.Room
import androidx.room.RoomDatabase
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
        // See the Android actual: Room's builder is `inline reified`, so the type parameter is
        // bound to the base type and the result cast back. Sound because [initializer] is always
        // provided, so Room never falls back to reflective construction.
        @Suppress("UNCHECKED_CAST")
        return Room.databaseBuilder<RoomDatabase>(
            name = "${documentsDirectory()}/$name",
            factory = { initializer() },
        ) as RoomDatabase.Builder<T>
    }

    /**
     * Documents rather than Caches: the system may evict Caches under storage pressure, which
     * would silently drop conversation history.
     */
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

/**
 * `Dispatchers.IO` is JVM-only — on Native it is internal, so this is the closest equivalent.
 *
 * Room serialises its own writes and the bundled SQLite driver does its own locking, so what
 * matters here is only that queries stay off the main thread.
 */
actual val databaseDispatcher: CoroutineContext = Dispatchers.Default
