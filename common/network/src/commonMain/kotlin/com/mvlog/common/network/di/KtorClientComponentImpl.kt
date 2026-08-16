package com.mvlog.common.network.di

import com.mvlog.log.TLogger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging

internal class KtorClientComponentImpl : KtorClientComponent {

    /**
     * One client for the whole app.
     *
     * Ktor clients own a connection pool and a thread pool, so creating them per call site is
     * expensive and leaks. No engine is named here on purpose — Ktor resolves it from whichever
     * engine artifact the platform brings, which keeps this file free of platform code.
     */
    private val httpClient = HttpClient {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) = TLogger.d(TAG, message)
            }
            // Headers carry API keys; bodies carry conversations. Neither belongs in a log.
            level = LogLevel.INFO
        }
    }

    override fun httpClient(): HttpClient = httpClient

    private companion object {
        const val TAG = "Http"
    }
}
