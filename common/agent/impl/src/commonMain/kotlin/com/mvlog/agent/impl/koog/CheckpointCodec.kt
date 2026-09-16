package com.mvlog.agent.impl.koog

import ai.koog.agents.snapshot.feature.AgentCheckpointData
import kotlinx.serialization.json.Json

internal class CheckpointCodec(
    private val json: Json,
) {

    fun encode(checkpoint: AgentCheckpointData): String =
        json.encodeToString(AgentCheckpointData.serializer(), checkpoint)

    fun decode(payload: String): AgentCheckpointData =
        json.decodeFromString(AgentCheckpointData.serializer(), payload)
}
