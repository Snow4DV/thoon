package com.mvlog.thoon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mvlog.chatslist.api.ChatsListScreen
import com.mvlog.navigation.screen.CollectedScreenFactories
import com.mvlog.thoon.startup.rememberAppStartup
import com.mvlog.ui.ThoonTheme
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator

/**
 * The app.
 *
 * Deliberately not `@Preview`-able: it installs a database context and initialises every feature,
 * which is not something a preview should do. Preview the individual screens instead.
 */
@Composable
fun App() {
    rememberAppStartup()

    ThoonTheme {
        CircuitCompositionLocals(rememberThoonCircuit()) {
            val backStack = rememberSaveableBackStack(root = ChatsListScreen)

            // The two-argument form: the single-argument overload is Android-only, and `onRootPop`
            // receives a `PopResult?`, so it cannot be written as `{ }`.
            val navigator = rememberCircuitNavigator(backStack) { _ -> }

            // No inset padding here: it would apply to every screen uniformly, which stops a top
            // bar from painting its background behind the status bar and leaves it floating below
            // a strip of window. Screens consume the insets they actually care about.
            NavigableCircuitContent(
                navigator = navigator,
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Assembles the screen graph.
 *
 * Deliberately names no feature: the routing table is whatever the initializers registered with
 * `ScreenFactoriesCollector`, so adding a screen means adding an initializer to `FeatureRegistry`
 * and nothing here. One dispatcher stands in for every feature's factories and resolves by screen
 * type, so a feature's component is not built until something navigates to it.
 *
 * The saver comes from the same registrations. It is built here, inside `remember`, which runs
 * after `rememberAppStartup()` has run every initializer — so the module it snapshots is complete.
 */
@Composable
private fun rememberThoonCircuit(): Circuit = remember {
    val screenFactories = CollectedScreenFactories()

    Circuit.Builder()
        .addPresenterFactory(screenFactories)
        .addUiFactory(screenFactories)
        .setCircuitSaver(screenFactories.circuitSaver())
        .build()
}
