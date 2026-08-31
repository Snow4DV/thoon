package com.mvlog.navigation.screen

import com.mvlog.log.TLogger
import com.slack.circuit.runtime.CircuitContext
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
