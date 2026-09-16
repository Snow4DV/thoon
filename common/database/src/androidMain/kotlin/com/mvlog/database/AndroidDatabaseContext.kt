package com.mvlog.database

import android.content.Context

object AndroidDatabaseContext {

    private var applicationContext: Context? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun require(): Context = requireNotNull(applicationContext) {
        "AndroidDatabaseContext.install(context) must be called by rememberAppStartup " +
            "before any DAO is read."
    }
}
