package com.mvlog.navigation.screen

import com.mvlog.navigation.CommonParcelize
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@CommonParcelize
private data object RegisteredScreen : Screen

@CommonParcelize
private data object UnregisteredScreen : Screen

class ScreenFactoriesCollectorTest {

    @AfterTest
    fun tearDown() = ScreenFactoriesCollector.reset()

    @Test
    fun theProviderIsNotInvokedUntilTheScreenIsAskedFor() {
        // The whole reason for keying by screen type. A collector that built eagerly would
        // construct every feature's component during startup, and the regression would be
        // invisible — nothing breaks, launch is just slower.
        var built = 0
        ScreenFactoriesCollector.collect<RegisteredScreen> {
            built++
            screenFactory()
        }

        assertEquals(0, built, "registering must not build anything")

        ScreenFactoriesCollector.obtain(RegisteredScreen::class)
        assertEquals(1, built)
    }

    @Test
    fun anUnclaimedScreenResolvesToNull() {
        ScreenFactoriesCollector.collect<RegisteredScreen> { screenFactory() }

        assertNotNull(ScreenFactoriesCollector.obtain(RegisteredScreen::class))
        assertNull(
            ScreenFactoriesCollector.obtain(UnregisteredScreen::class),
            "an unclaimed screen must fall through to Circuit's unavailable content",
        )
    }

    @Test
    fun readingDoesNotConsume() {
        // The opposite of AppOnCreateActionsCollector, which drains because its actions run once.
        // Draining here would route the first navigation and lose every one after it.
        ScreenFactoriesCollector.collect<RegisteredScreen> { screenFactory() }

        assertNotNull(ScreenFactoriesCollector.obtain(RegisteredScreen::class))
        assertNotNull(ScreenFactoriesCollector.obtain(RegisteredScreen::class))
    }

    @Test
    fun aScreenResolvesToTheFactoryItsFeatureRegistered() {
        val expected = screenFactory()
        ScreenFactoriesCollector.collect<RegisteredScreen> { expected }

        assertSame(expected, ScreenFactoriesCollector.obtain(RegisteredScreen::class))
    }

    private fun screenFactory() = ScreenFactory(
        presenterFactory = NoOpPresenterFactory,
        uiFactory = NoOpUiFactory,
    )

    private object NoOpPresenterFactory : Presenter.Factory {
        override fun create(
            screen: Screen,
            navigator: Navigator,
            context: CircuitContext,
        ): Presenter<*>? = null
    }

    private object NoOpUiFactory : Ui.Factory {
        override fun create(screen: Screen, context: CircuitContext): Ui<*>? = null
    }
}
