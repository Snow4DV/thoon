package com.mvlog.agenttools.file

import android.content.Context
import okio.Path
import okio.Path.Companion.toPath

/**
 * Holds the application [Context] needed to locate app-private storage.
 *
 * A second context holder alongside `AndroidDatabaseContext` — a wart, kept rather than removed,
 * because the alternative is a dependency from the database module to this one purely to share a
 * field. Both are installed from the same place in `AppStartup.android.kt`, so the ordering stays
 * visible in one file.
 */
object AndroidFilesContext {

    private var applicationContext: Context? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun require(): Context = requireNotNull(applicationContext) {
        "AndroidFilesContext.install(context) must be called before the agent uses a file tool."
    }
}

internal actual fun platformFilesRoot(): Path =
    AndroidFilesContext.require().filesDir.absolutePath.toPath()
