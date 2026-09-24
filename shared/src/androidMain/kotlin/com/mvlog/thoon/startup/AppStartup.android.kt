package com.mvlog.thoon.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mvlog.agenttools.file.AndroidFilesContext
import com.mvlog.database.AndroidDatabaseContext
import com.mvlog.sharedpreferences.impl.AndroidPreferencesContext

// Every holder keeps applicationContext, so the Activity is not retained.
@Composable
actual fun rememberAppStartup() {
    val context = LocalContext.current

    remember(context) {
        AndroidDatabaseContext.install(context)
        AndroidFilesContext.install(context)
        AndroidPreferencesContext.install(context)
        AppStartup.start()
    }
}
