package com.mvlog.coroutines.di

import com.mvlog.coroutines.dispatcher.CoroutineDispatchers

interface CoroutineDispatchersComponent {

    fun dispatchers() : CoroutineDispatchers
}
