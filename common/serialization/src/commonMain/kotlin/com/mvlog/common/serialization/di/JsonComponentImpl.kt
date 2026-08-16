package com.mvlog.common.serialization.di

import kotlinx.serialization.json.Json

internal class JsonComponentImpl : JsonComponent {

    private val json = Json { ignoreUnknownKeys = true }

    override fun json(): Json {
        return json
    }
}
