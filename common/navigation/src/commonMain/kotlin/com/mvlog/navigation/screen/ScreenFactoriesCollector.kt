package com.mvlog.navigation.screen

import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.screen.CircuitSaveable
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

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

    inline fun <reified T : Screen> collect(
        serializer: KSerializer<T>,
        noinline provider: () -> ScreenFactory,
    ) {
        collect(T::class, serializer, provider)
    }

    @PublishedApi
    internal fun <T : Screen> collect(
        screen: KClass<T>,
        serializer: KSerializer<T>,
        provider: () -> ScreenFactory,
    ) {
        registrations[screen] = Registration(screen, serializer, provider)
    }

    fun obtain(screen: KClass<out Screen>): ScreenFactory? = registrations[screen]?.provider?.invoke()

    fun serializersModule(): SerializersModule = SerializersModule {
        polymorphic(CircuitSaveable::class) {
            registrations.values.forEach { it.registerInto(this) }
        }
    }

    fun reset() {
        registrations.clear()
    }
}
