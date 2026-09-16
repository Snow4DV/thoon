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

/**
 * The one factory pair the app registers, standing in for every feature's.
 *
 * Circuit resolves a screen by scanning its factory lists and asking each in turn, so registering
 * every feature directly would mean building all of them to open one screen. This looks the screen
 * up instead, and only the owning feature's factory is ever constructed.
 *
 * One class implements both interfaces because their `create` overloads differ in arity; the same
 * instance is registered as a presenter factory and as a UI factory.
 */
class CollectedScreenFactories : Presenter.Factory, Ui.Factory {

    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? = screen.factoryOrNull()?.presenterFactory?.create(screen, navigator, context)

    override fun create(screen: Screen, context: CircuitContext): Ui<*>? =
        screen.factoryOrNull()?.uiFactory?.create(screen, context)

    /**
     * How the back stack is persisted: kotlinx-serialization over the screens the collector knows.
     *
     * Built from the same registrations that route screens, so a screen cannot be routable without
     * being saveable. A record that fails to restore — a screen renamed since the state was saved —
     * is dropped by Circuit rather than crashing; it is logged here so a stack that comes back one
     * screen short has an explanation.
     */
    fun circuitSaver(): CircuitSaver = SerializableCircuitSaver(
        configuration = SavedStateConfiguration {
            serializersModule = ScreenFactoriesCollector.serializersModule()
        },
        onRestoreError = { error -> TLogger.e(TAG, "Dropped a saved screen: ${error.message}") },
    )

    /**
     * Logged rather than silent: a feature that forgets to register renders Circuit's
     * `onUnavailableContent`, which looks like a blank screen with no error anywhere.
     */
    private fun Screen.factoryOrNull(): ScreenFactory? =
        ScreenFactoriesCollector.obtain(this::class)
            ?: null.also { TLogger.e(TAG, "No feature registered a factory for ${this::class.simpleName}") }

    private companion object {
        const val TAG = "Navigation"
    }
}
