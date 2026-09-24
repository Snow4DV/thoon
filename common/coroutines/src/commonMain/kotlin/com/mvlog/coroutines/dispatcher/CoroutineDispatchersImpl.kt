package com.mvlog.coroutines.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal expect val ioDispatcher: CoroutineDispatcher

internal class CoroutineDispatchersImpl : CoroutineDispatchers {
    override val default: CoroutineDispatcher
        get() = Dispatchers.Default
    override val io: CoroutineDispatcher
        get() = ioDispatcher
    override val main: CoroutineDispatcher
        get() = Dispatchers.Main
    override val immediate: CoroutineDispatcher
        get() = Dispatchers.Main.immediate
}
