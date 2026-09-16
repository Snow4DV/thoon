package com.mvlog.thoon.startup

import androidx.compose.runtime.Composable
import com.mvlog.init.start.AppOnCreateActionsCollector
import com.mvlog.thoon.initialization.FeatureRegistry

internal object AppStartup {

    private var started = false

    // Not synchronised: only ever called from composition, and the collectors are not thread-safe
    // either.
    fun start() {
        if (started) return
        started = true

        FeatureRegistry().initialize()
        AppOnCreateActionsCollector.execute()
    }
}

// Synchronous end to end: anything composed after this may resolve holders.
@Composable
expect fun rememberAppStartup()
