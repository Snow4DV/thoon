package com.mvlog.thoon.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * No platform handle to install: the iOS `DatabaseBuilderFactory` resolves `NSDocumentDirectory`
 * itself, so starting the app is all there is to do here.
 */
@Composable
actual fun rememberAppStartup() {
    remember { AppStartup.start() }
}
