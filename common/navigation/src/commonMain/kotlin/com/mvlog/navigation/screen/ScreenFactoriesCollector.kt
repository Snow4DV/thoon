package com.mvlog.navigation.screen

import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.CircuitSaveable
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * Where features register the screens they own.
 *
 * Collect from a base initializer, alongside the component holder it sets:
 *
 * ```kotlin
 * ScreenFactoriesCollector.collect(ChatScreen.serializer()) { ChatComponentHolder.get().screenFactory() }
 * ```
 *
 * One registration does two jobs. The provider is how the screen is *opened*; the serializer is how
 * it is *saved*, since Circuit persists the back stack through kotlinx-serialization and must be told
 * every concrete `Screen` type up front. They are one call rather than two because a screen that can
 * be navigated to but not saved is a crash waiting for a configuration change: since Circuit 0.38 an
 * unregistered value fails the save instead of being dropped. Deriving the type from the serializer
 * also means it cannot be registered under the wrong class.
 *
 * This is the hand-rolled equivalent of what `@CircuitSerializable` and `@CircuitInject` generate
 * into a DI multibinding in Slack's own apps; the collector is the multibinding.
 *
 * **Keyed by screen type, and the provider is not invoked until something navigates there.** That is
 * the point: a flat list of factories would have to be built in full to open a single screen, which
 * for this app means constructing every feature's component — and through them the agent subsystem —
 * during startup.
 *
 * Keying by [KClass] follows the framework rather than working around it: `Circuit.Builder` keys its
 * own `animatedScreenTransforms` the same way.
 *
 * As with `AgentToolsCollector`, and unlike `AppOnCreateActionsCollector`, reading does **not**
 * drain: start-up actions run once, but this is read every time a screen is opened.
 */
object ScreenFactoriesCollector {

    private class Registration<T : Screen>(
        val screen: KClass<T>,
        val serializer: KSerializer<T>,
        val provider: () -> ScreenFactory,
    ) {
        fun registerInto(builder: PolymorphicModuleBuilder<CircuitSaveable>) {
            builder.subclass(screen, serializer)
        }
    }

    private val registrations = mutableMapOf<KClass<out Screen>, Registration<*>>()

    /**
     * Should only be invoked in base initializers!
     */
    inline fun <reified T : Screen> collect(
        serializer: KSerializer<T>,
        noinline provider: () -> ScreenFactory,
    ) {
        collect(T::class, serializer, provider)
    }

    /**
     * Should only be invoked in base initializers!
     *
     * Prefer the reified overload; this exists for it to call.
     */
    fun <T : Screen> collect(
        screen: KClass<T>,
        serializer: KSerializer<T>,
        provider: () -> ScreenFactory,
    ) {
        registrations[screen] = Registration(screen, serializer, provider)
    }

    /**
     * The factory for [screen], built on first use, or null if no feature claimed it.
     *
     * Matched by exact class, where a hand-written Circuit factory would match with `is`. Every
     * concrete screen therefore registers itself; a sealed hierarchy registered under its base type
     * would not resolve.
     */
    fun obtain(screen: KClass<out Screen>): ScreenFactory? = registrations[screen]?.provider?.invoke()

    /**
     * Every registered screen as a polymorphic subtype of [CircuitSaveable], which is the base type
     * Circuit's saver serializes against. Read once, when the `Circuit` is built — after every
     * initializer has run.
     */
    fun serializersModule(): SerializersModule = SerializersModule {
        polymorphic(CircuitSaveable::class) {
            registrations.values.forEach { it.registerInto(this) }
        }
    }

    /**
     * Only for testing — a process registers its screens once, at startup.
     */
    fun reset() {
        registrations.clear()
    }
}
