package com.mvlog.agent.impl.koog

import ai.koog.agents.snapshot.feature.AgentCheckpointData
import kotlinx.serialization.json.Json

/**
 * The only place that knows a stored checkpoint is an [AgentCheckpointData].
 *
 * The shared `Json` must keep `ignoreUnknownKeys` on — that is not cosmetic here. The library's own
 * serializer migrates older checkpoint payloads and relies on tolerant decoding to do it.
 */
internal class CheckpointCodec(
    private val json: Json,
) {

    fun encode(checkpoint: AgentCheckpointData): String =
        json.encodeToString(AgentCheckpointData.serializer(), checkpoint)

    fun decode(payload: String): AgentCheckpointData =
        json.decodeFromString(AgentCheckpointData.serializer(), payload)
}
