package com.mvlog.common.network.di

import com.mvlog.log.TLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

internal class KtorClientComponentImpl : KtorClientComponent {

    // One client per process: a Ktor client owns a connection pool and a thread pool.
    private val httpClient = HttpClient {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) = TLogger.d(TAG, message)
            }
            level = LogLevel.INFO
        }
    }

    override fun httpClient(): HttpClient = httpClient

    private companion object {
        const val TAG = "Http"
    }
}
