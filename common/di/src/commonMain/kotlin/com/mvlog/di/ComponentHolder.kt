package com.mvlog.di

interface ComponentHolder<T : Any> {

    fun get(): T

    fun reset()

    fun set(componentProvider: () -> T)

    /**
     * Only for testing
     */
    fun set(component: T)
}
