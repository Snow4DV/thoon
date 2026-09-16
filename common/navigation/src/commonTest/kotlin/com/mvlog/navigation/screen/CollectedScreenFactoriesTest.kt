package com.mvlog.navigation.screen

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.Serializable

@Serializable
private data object OwnedScreen : Screen

private data object OrphanScreen : Screen

class CollectedScreenFactoriesTest {

    private val factories = CollectedScreenFactories()

    private var presenterAskedFor: Screen? = null
    private var uiAskedFor: Screen? = null

    @AfterTest
    fun tearDown() = ScreenFactoriesCollector.reset()

    @Test
    fun aRegisteredScreenReachesTheFactoryItsFeatureRegistered() {
        registerOwnedScreen()

        factories.create(OwnedScreen, Navigator.NoOp, CircuitContext.EMPTY)
        factories.create(OwnedScreen, CircuitContext.EMPTY)

        assertEquals(OwnedScreen, presenterAskedFor, "the presenter half must be routed")
        assertEquals(OwnedScreen, uiAskedFor, "the ui half must be routed")
    }

    @Test
    fun aScreenNobodyRegisteredResolvesToNullWithoutAskingAnyone() {
        registerOwnedScreen()

        assertNull(factories.create(OrphanScreen, Navigator.NoOp, CircuitContext.EMPTY))
        assertNull(factories.create(OrphanScreen, CircuitContext.EMPTY))

        assertNull(presenterAskedFor, "a foreign screen must not reach another feature's factory")
        assertNull(uiAskedFor)
    }

    // Records which factory was asked rather than what came back: a real Presenter needs a
    // composition.
    private fun registerOwnedScreen() {
        ScreenFactoriesCollector.collect(OwnedScreen.serializer()) {
            ScreenFactory(
                presenterFactory = { screen, _, _ -> presenterAskedFor = screen; null },
                uiFactory = { screen, _ -> uiAskedFor = screen; null },
            )
        }
    }
}
