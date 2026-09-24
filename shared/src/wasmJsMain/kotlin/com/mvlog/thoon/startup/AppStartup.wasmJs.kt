package com.mvlog.thoon.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAppStartup() {
    remember { AppStartup.start() }
}
