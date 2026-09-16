package com.mvlog.agent.impl.data.mapper

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.impl.data.room.entity.AgentConfigEntity
import com.mvlog.agent.impl.data.serialization.AgentConfigKind
import com.mvlog.agent.impl.data.serialization.StoredAgentConfigPayload
import kotlinx.serialization.json.Json

internal class AgentConfigMapper(
    private val json: Json,
) {

    fun toDomain(entity: AgentConfigEntity): AgentConfig {
        val payload = json.decodeFromString(StoredAgentConfigPayload.serializer(), entity.payloadJson)
        val id = AgentConfigId(entity.id)

        return when (payload) {
            is StoredAgentConfigPayload.OpenAiCompatible -> AgentConfig.OpenAiCompatible(
                id = id,
                name = entity.name,
                modelId = entity.modelId,
                baseUrl = payload.baseUrl,
                apiKey = payload.apiKey,
                chatCompletionsPath = payload.chatCompletionsPath,
            )

            is StoredAgentConfigPayload.Anthropic -> AgentConfig.Anthropic(
                id = id,
                name = entity.name,
                modelId = entity.modelId,
                baseUrl = payload.baseUrl,
                apiKey = payload.apiKey,
            )

            is StoredAgentConfigPayload.Ollama -> AgentConfig.Ollama(
                id = id,
                name = entity.name,
                modelId = entity.modelId,
                baseUrl = payload.baseUrl,
            )

            is StoredAgentConfigPayload.Local -> AgentConfig.Local(
                id = id,
                name = entity.name,
                modelId = entity.modelId,
                engineId = payload.engineId,
            )
        }
    }

    fun toEntity(id: AgentConfigId, draft: AgentConfigDraft): AgentConfigEntity = AgentConfigEntity(
        id = id.value,
        name = draft.name.trim(),
        kind = kindOf(draft),
        modelId = draft.modelId.trim(),
        payloadJson = json.encodeToString(StoredAgentConfigPayload.serializer(), payloadOf(draft)),
    )

    private fun kindOf(draft: AgentConfigDraft): String = when (draft) {
        is AgentConfigDraft.OpenAiCompatible -> AgentConfigKind.OPENAI_COMPATIBLE
        is AgentConfigDraft.Anthropic -> AgentConfigKind.ANTHROPIC
        is AgentConfigDraft.Ollama -> AgentConfigKind.OLLAMA
        is AgentConfigDraft.Local -> AgentConfigKind.LOCAL
    }

    private fun payloadOf(draft: AgentConfigDraft): StoredAgentConfigPayload = when (draft) {
        is AgentConfigDraft.OpenAiCompatible -> StoredAgentConfigPayload.OpenAiCompatible(
            baseUrl = draft.baseUrl?.trim()?.ifBlank { null },
            apiKey = draft.apiKey.trim(),
            chatCompletionsPath = draft.chatCompletionsPath?.trim()?.ifBlank { null },
        )

        is AgentConfigDraft.Anthropic -> StoredAgentConfigPayload.Anthropic(
            baseUrl = draft.baseUrl?.trim()?.ifBlank { null },
            apiKey = draft.apiKey.trim(),
        )

        // Required, so never collapsed to null like the optional URLs above.
        is AgentConfigDraft.Ollama -> StoredAgentConfigPayload.Ollama(
            baseUrl = draft.baseUrl.trim(),
        )

        is AgentConfigDraft.Local -> StoredAgentConfigPayload.Local(
            engineId = draft.engineId.trim(),
        )
    }
}
