package com.mvlog.navigation.screen

import com.slack.circuit.runtime.screen.Screen
import kotlin.reflect.KClass

/**
 * Where features register the screens they own.
 *
 * Collect from a base initializer, alongside the component holder it sets:
 *
 * ```kotlin
 * ScreenFactoriesCollector.collect<ChatScreen> { ChatComponentHolder.get().screenFactory() }
 * ```
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

    private val providers = mutableMapOf<KClass<out Screen>, () -> ScreenFactory>()

    /**
     * Should only be invoked in base initializers!
     */
    inline fun <reified T : Screen> collect(noinline provider: () -> ScreenFactory) {
        collect(T::class, provider)
    }

    /**
     * Should only be invoked in base initializers!
     *
     * Prefer the reified overload; this exists for it to call.
     */
    fun collect(screen: KClass<out Screen>, provider: () -> ScreenFactory) {
        providers[screen] = provider
    }

    /**
     * The factory for [screen], built on first use, or null if no feature claimed it.
     *
     * Matched by exact class, where a hand-written Circuit factory would match with `is`. Every
     * concrete screen therefore registers itself; a sealed hierarchy registered under its base type
     * would not resolve.
     */
    fun obtain(screen: KClass<out Screen>): ScreenFactory? = providers[screen]?.invoke()

    /**
     * Only for testing — a process registers its screens once, at startup.
     */
    fun reset() {
        providers.clear()
    }
}
