package com.mvlog.di

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Component that is used in modules that provide implementation through build method
 * and not through set of provider in base initializer
 */
abstract class LazyComponentHolder<T : Any> : ComponentHolder<T> {

    private val lock = SynchronizedObject()

    private var component: T? = null

    abstract fun build(): T

    override fun get(): T {
        return component ?: synchronized(lock) {
            build().also { this.component = it }
        }
    }

    override fun reset() {
        component = null
    }
}
