package com.mvlog.agent.impl.fake

import kotlinx.serialization.json.Json

/** ignoreUnknownKeys matches the app's Json; Koog's checkpoint reads depend on it. */
internal val TestJson = Json { ignoreUnknownKeys = true }
