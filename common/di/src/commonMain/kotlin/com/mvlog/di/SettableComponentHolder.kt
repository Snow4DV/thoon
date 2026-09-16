package com.mvlog.di

interface SettableComponentHolder<T : Any> : ComponentHolder<T> {

    fun set(componentProvider: () -> T)

    fun set(component: T)
}
