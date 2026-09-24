package com.mvlog.agenttools.file

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal object UnconfinedDispatchers : CoroutineDispatchers {
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
    override val io: CoroutineDispatcher = Dispatchers.Unconfined
    override val main: CoroutineDispatcher = Dispatchers.Unconfined
    override val immediate: CoroutineDispatcher = Dispatchers.Unconfined
}
