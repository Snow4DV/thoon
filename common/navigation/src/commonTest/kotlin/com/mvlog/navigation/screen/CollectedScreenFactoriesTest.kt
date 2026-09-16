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

/**
 * The dispatcher is what Circuit actually holds, so this covers the seam between a framework that
 * scans a flat list and a registry that looks up by type.
 *
 * Asserted by which factory got asked rather than by what came back: building a real `Presenter`
 * needs a composition, and routing is the only thing this class does.
 */
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

        // Circuit falls back to `onUnavailableContent` on null. Throwing here would turn a feature
        // that forgot to register into a crash the moment someone navigated.
        assertNull(factories.create(OrphanScreen, Navigator.NoOp, CircuitContext.EMPTY))
        assertNull(factories.create(OrphanScreen, CircuitContext.EMPTY))

        assertNull(presenterAskedFor, "a foreign screen must not reach another feature's factory")
        assertNull(uiAskedFor)
    }

    private fun registerOwnedScreen() {
        ScreenFactoriesCollector.collect(OwnedScreen.serializer()) {
            ScreenFactory(
                presenterFactory = { screen, _, _ -> presenterAskedFor = screen; null },
                uiFactory = { screen, _ -> uiAskedFor = screen; null },
            )
        }
    }
}
