package com.mvlog.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.w3c.dom.Worker
import kotlin.coroutines.CoroutineContext

actual class DatabaseBuilderFactory {

    actual fun <T : RoomDatabase> builder(
        name: String,
        initializer: () -> T,
    ): RoomDatabase.Builder<T> {
        // Sound: the initializer is always passed, so Room never takes its reified reflective path.
        @Suppress("UNCHECKED_CAST")
        return Room.databaseBuilder<RoomDatabase>(name = name, factory = { initializer() })
            as RoomDatabase.Builder<T>
    }
}

actual val databaseDispatcher: CoroutineContext = Dispatchers.Default

internal actual fun platformSQLiteDriver(): SQLiteDriver = WebWorkerSQLiteDriver(sqliteWorker())

@OptIn(ExperimentalWasmJsInterop::class)
private fun sqliteWorker(): Worker =
    js("""new Worker(new URL("thoon-sqlite-worker/worker.js", import.meta.url))""")
