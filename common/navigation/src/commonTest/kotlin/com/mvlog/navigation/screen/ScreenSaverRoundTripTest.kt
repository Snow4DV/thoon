package com.mvlog.navigation.screen

import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.CircuitSaveable
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SavedScreen(val id: String, val position: Long? = null) : Screen

private data object NeverRegisteredScreen : Screen

/**
 * The half of persistence this module owns: that registering a screen makes it a known subtype of
 * [CircuitSaveable], which is the base type Circuit's saver encodes against.
 *
 * Driven through kotlinx JSON rather than the saver's own `save`, because that writes into
 * `android.os.Bundle`, a stub in host tests. The polymorphic module is the same object either way,
 * so what is proven here is the registration; the Bundle layer is androidx's and is checked on the
 * emulator by killing the process with a chat open.
 */
class ScreenSaverRoundTripTest {

    @AfterTest
    fun tearDown() = ScreenFactoriesCollector.reset()

    @Test
    fun aRegisteredScreenRoundTripsAsACircuitSaveable() {
        ScreenFactoriesCollector.collect(SavedScreen.serializer()) { noOpFactory() }
        val json = Json { serializersModule = ScreenFactoriesCollector.serializersModule() }
        val polymorphic = PolymorphicSerializer(CircuitSaveable::class)

        val screen = SavedScreen(id = "chat-7", position = 42L)
        val restored = json.decodeFromString(polymorphic, json.encodeToString(polymorphic, screen))

        assertEquals(screen, restored, "a registered screen must come back equal, id and position intact")
    }

    @Test
    fun anUnregisteredScreenIsRefusedRatherThanSavedBlind() {
        val saver = CollectedScreenFactories().circuitSaver()

        assertNull(
            runCatching { saver.save(NeverRegisteredScreen) }.getOrNull(),
            "a screen nobody registered must not be persisted in a shape nothing can restore",
        )
    }

    private fun noOpFactory() = ScreenFactory(
        presenterFactory = object : Presenter.Factory {
            override fun create(screen: Screen, navigator: Navigator, context: CircuitContext): Presenter<*>? = null
        },
        uiFactory = object : Ui.Factory {
            override fun create(screen: Screen, context: CircuitContext): Ui<*>? = null
        },
    )
}
