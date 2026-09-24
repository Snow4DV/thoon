package com.mvlog.agenttools.file

import android.content.Context
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/** Installed from `AppStartup.android.kt`, beside `AndroidDatabaseContext`. */
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

internal actual fun platformFileSystem(): FileSystem = FileSystem.SYSTEM
