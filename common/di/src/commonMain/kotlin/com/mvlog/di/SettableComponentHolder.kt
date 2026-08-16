package com.mvlog.di

interface SettableComponentHolder<T : Any> : ComponentHolder<T> {

    fun set(componentProvider: () -> T)

    /**
     * Only for testing
     */
    fun set(component: T)
}
