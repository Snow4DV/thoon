package com.mvlog.agenttools.di

import com.mvlog.common.network.di.KtorClientComponentHolder
import com.mvlog.coroutines.di.CoroutineDispatchersComponentHolder
import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import io.ktor.client.HttpClient

internal interface AgentToolsComponentDependencies {

    val httpClient: HttpClient
    val dispatchers: CoroutineDispatchers

    class Impl : AgentToolsComponentDependencies {
        override val httpClient: HttpClient
            get() = KtorClientComponentHolder.get().httpClient()

        override val dispatchers: CoroutineDispatchers
            get() = CoroutineDispatchersComponentHolder.get().dispatchers()
    }
}
