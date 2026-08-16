package com.mvlog.di

interface ComponentHolder<T : Any> {

    fun get(): T

    fun reset()
}
