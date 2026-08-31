package com.mvlog.thoon.startup

import androidx.compose.runtime.Composable
import com.mvlog.init.start.AppOnCreateActionsCollector
import com.mvlog.thoon.initialization.FeatureRegistry

/**
 * Brings the app's components up, once per process.
 *
 * Two phases, and the order matters: initializers register providers with their holders, and only
 * then can the collected on-create actions — which resolve those holders — run.
 */
internal object AppStartup {

    private var started = false

    /**
     * Not synchronised: this is called from composition, which is single-threaded, and the
     * collector it drives is not thread-safe either.
     */
    fun start() {
        if (started) return
        started = true

        FeatureRegistry().initialize()
        AppOnCreateActionsCollector.execute()
    }
}

/**
 * Runs [AppStartup] for the current platform, plus whatever that platform needs installed first.
 *
 * Composable because it is the one place both platforms share — Android enters through
 * `setContent { App() }` and iOS through `ComposeUIViewController { App() }`, so there is no common
 * non-UI entry point to hang this on.
 *
 * Returns nothing: both phases are synchronous, so there is no intermediate "not ready" state to
 * report. Composable calls run in order, so anything after this line can resolve holders safely.
 */
@Composable
expect fun rememberAppStartup()
