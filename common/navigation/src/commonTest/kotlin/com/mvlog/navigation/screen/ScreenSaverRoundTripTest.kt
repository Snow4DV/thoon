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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class SavedScreen(val id: String, val position: Long? = null) : Screen

private data object NeverRegisteredScreen : Screen

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
    fun anUnregisteredScreenIsUnknownToTheSaver() {
        ScreenFactoriesCollector.collect(SavedScreen.serializer()) { noOpFactory() }
        val module = ScreenFactoriesCollector.serializersModule()

        assertNotNull(module.getPolymorphic(CircuitSaveable::class, SavedScreen("x")))
        assertNull(
            module.getPolymorphic(CircuitSaveable::class, NeverRegisteredScreen),
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
