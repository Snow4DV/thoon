package com.mvlog.database

import android.content.Context

/**
 * Holds the application [Context] that Room needs to resolve a database file.
 *
 * Must be installed from `Application.onCreate` before anything reads a DAO. Deliberately explicit
 * rather than an androidx.startup provider: ordering stays visible in the app's own startup code,
 * and no extra ContentProvider runs at process launch.
 */
object AndroidDatabaseContext {

    private var applicationContext: Context? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun require(): Context = requireNotNull(applicationContext) {
        "AndroidDatabaseContext.install(context) must be called from Application.onCreate " +
            "before any DAO is used."
    }
}
