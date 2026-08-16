package com.mvlog.coroutines.di

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import com.mvlog.coroutines.dispatcher.CoroutineDispatchersImpl

internal class CoroutineDispatchersComponentImpl : CoroutineDispatchersComponent {

    private val dispatchers = CoroutineDispatchersImpl()

    override fun dispatchers(): CoroutineDispatchers = dispatchers
}
