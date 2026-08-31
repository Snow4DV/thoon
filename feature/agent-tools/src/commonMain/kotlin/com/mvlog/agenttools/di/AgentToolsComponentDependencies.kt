package com.mvlog.agenttools.di

import com.mvlog.common.network.di.KtorClientComponentHolder
import com.mvlog.coroutines.di.CoroutineDispatchersComponentHolder
import com.mvlog.coroutines.dispatcher.CoroutineDispatchers
import io.ktor.client.HttpClient

/**
 * What this feature pulls from the rest of the app.
 *
 * Per-access getters rather than stored values, so nothing is resolved before the holders that
 * provide it have been given providers.
 */
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
