package com.mvlog.database

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual class DatabaseBuilderFactory {

    actual fun <T : RoomDatabase> builder(
        name: String,
        initializer: () -> T,
    ): RoomDatabase.Builder<T> {
        val context = AndroidDatabaseContext.require()
        // Resolved through the platform rather than assembled by hand so the file lands where
        // Android expects it (backup rules, `adb` tooling, per-user storage).
        val path = context.getDatabasePath(name).absolutePath

        // Room's builder is `inline reified`, which a generic `expect` member cannot satisfy.
        // Binding Room's type parameter to the base type sidesteps that: the reified type is only
        // used to reflectively locate a constructor when no factory is given, and [initializer] is
        // always supplied here. `build()` therefore returns exactly what [initializer] produced.
        @Suppress("UNCHECKED_CAST")
        return Room.databaseBuilder<RoomDatabase>(context, path) { initializer() }
            as RoomDatabase.Builder<T>
    }
}

actual val databaseDispatcher: CoroutineContext = Dispatchers.IO
