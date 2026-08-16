package com.mvlog.common.network.di

import io.ktor.client.HttpClient

interface KtorClientComponent {

    fun httpClient(): HttpClient
}
