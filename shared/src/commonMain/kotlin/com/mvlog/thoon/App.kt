package com.mvlog.thoon

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.colors
import com.composeunstyled.theme.Theme
import com.mvlog.chatslist.api.ChatsListScreen
import com.mvlog.navigation.screen.CollectedScreenFactories
import com.mvlog.thoon.startup.rememberAppStartup
import com.mvlog.thoon.theme.ApplySystemAppearance
import com.mvlog.ui.ThoonTheme
import com.mvlog.usersettings.api.di.UserSettingsComponentHolder
import com.mvlog.usersettings.api.model.isDark
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator

// Not @Preview-able on purpose: it installs a database context and runs every initializer.
@Composable
fun App() {
    rememberAppStartup()

    val themeMode by remember {
        UserSettingsComponentHolder.get().observeThemeModeUseCase()()
    }.collectAsState()
    val isDark = themeMode.isDark(systemIsDark = isSystemInDarkTheme())
    ApplySystemAppearance(themeMode, isDark)

    ThoonTheme(isDark = isDark) {
        CircuitCompositionLocals(rememberThoonCircuit()) {
            val backStack = rememberSaveableBackStack(root = ChatsListScreen)

            // The two-argument form: the single-argument overload is Android-only, and `onRootPop`
            // receives a `PopResult?`, so it cannot be written as `{ }`.
            val navigator = rememberCircuitNavigator(backStack) { _ -> }

            // No inset padding here: applied uniformly it stops a top bar painting behind the
            // status bar.
            NavigableCircuitContent(
                navigator = navigator,
                backStack = backStack,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Theme[colors][backgroundColor]),
            )
        }
    }
}

// Inside remember so the saver snapshots the collector after every initializer has run.
@Composable
private fun rememberThoonCircuit(): Circuit = remember {
    val screenFactories = CollectedScreenFactories()

    Circuit.Builder()
        .addPresenterFactory(screenFactories)
        .addUiFactory(screenFactories)
        .setCircuitSaver(screenFactories.circuitSaver())
        .build()
}
