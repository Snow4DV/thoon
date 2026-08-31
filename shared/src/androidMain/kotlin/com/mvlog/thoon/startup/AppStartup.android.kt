package com.mvlog.thoon.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mvlog.agenttools.file.AndroidFilesContext
import com.mvlog.database.AndroidDatabaseContext

/**
 * Installs the `Context` Room and the agent's file tools need, then starts the app.
 *
 * Both holders take the application context themselves, so handing them an Activity is safe and
 * nothing outlives the process.
 */
@Composable
actual fun rememberAppStartup() {
    val context = LocalContext.current

    remember(context) {
        AndroidDatabaseContext.install(context)
        // A second holder rather than one shared one: the agent's file tools live in a feature
        // module the database module must not depend on. Installed together so the ordering stays
        // visible in one place.
        AndroidFilesContext.install(context)
        AppStartup.start()
    }
}
