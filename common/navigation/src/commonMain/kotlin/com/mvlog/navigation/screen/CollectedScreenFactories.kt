package com.mvlog.navigation.screen

import androidx.savedstate.serialization.SavedStateConfiguration
import com.mvlog.log.TLogger
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.CircuitSaver
import com.slack.circuit.serialization.SerializableCircuitSaver
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui

class CollectedScreenFactories : Presenter.Factory, Ui.Factory {

    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? = screen.factoryOrNull()?.presenterFactory?.create(screen, navigator, context)

    override fun create(screen: Screen, context: CircuitContext): Ui<*>? =
        screen.factoryOrNull()?.uiFactory?.create(screen, context)

    fun circuitSaver(): CircuitSaver = SerializableCircuitSaver(
        configuration = SavedStateConfiguration {
            serializersModule = ScreenFactoriesCollector.serializersModule()
        },
        onRestoreError = { error -> TLogger.e(TAG, "Dropped a saved screen: ${error.message}") },
    )

    // Logged because a miss renders Circuit's onUnavailableContent: a blank screen with no error
    // anywhere.
    private fun Screen.factoryOrNull(): ScreenFactory? =
        ScreenFactoriesCollector.obtain(this::class)
            ?: null.also { TLogger.e(TAG, "No feature registered a factory for ${this::class.simpleName}") }

    private companion object {
        const val TAG = "Navigation"
    }
}
