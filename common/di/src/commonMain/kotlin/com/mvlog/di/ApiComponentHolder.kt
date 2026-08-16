package com.mvlog.di

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Component that is set in `BaseInitiailizer` and is only reset explicitly
 */
abstract class ApiComponentHolder<T : Any> : SettableComponentHolder<T> {

    private val lock = SynchronizedObject()

    private var component: T? = null

    private var componentProvider: (() -> T)? = null

    override fun get(): T {
        return component ?: synchronized(lock) {
            componentProvider?.invoke()?.also { this.component = it }
                ?: error("Holder ${this::class.simpleName} doesn't have a provider")
        }
    }

    override fun reset() {
        component = null
    }

    override fun set(componentProvider: () -> T) {
        this.componentProvider = componentProvider
    }

    override fun set(component: T) {
        this.component = component
    }
}
