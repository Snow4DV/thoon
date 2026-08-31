package com.mvlog.agent.impl.fake

import kotlinx.serialization.json.Json

/**
 * Same shape as the shared instance from `common:serialization`.
 *
 * `ignoreUnknownKeys` is not cosmetic: the agent framework's own serialisers rely on tolerant
 * decoding to migrate older payloads, so a strict instance here would test something the app never
 * uses.
 */
internal val TestJson = Json { ignoreUnknownKeys = true }
