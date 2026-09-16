package com.mvlog.agent.impl.domain.usecase

import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigError

internal class ValidateAgentConfigDraftUseCase {

    operator fun invoke(draft: AgentConfigDraft): AgentConfigError? {
        if (draft.name.isBlank()) return AgentConfigError.BlankName
        if (draft.modelId.isBlank()) return AgentConfigError.BlankModelId

        return when (draft) {
            is AgentConfigDraft.OpenAiCompatible -> {
                if (draft.apiKey.isBlank()) return AgentConfigError.MissingApiKey
                draft.baseUrl.validateAsUrl()
            }

            is AgentConfigDraft.Anthropic -> {
                if (draft.apiKey.isBlank()) return AgentConfigError.MissingApiKey
                draft.baseUrl.validateAsUrl()
            }

            // Endpoint required: Koog's default is localhost, which on a device is the device.
            is AgentConfigDraft.Ollama -> {
                if (draft.baseUrl.isBlank()) return AgentConfigError.MissingBaseUrl
                draft.baseUrl.validateAsUrl()
            }

            is AgentConfigDraft.Local ->
                if (draft.engineId.isBlank()) AgentConfigError.BlankEngineId else null
        }
    }

    // Scheme + host only: stricter checks reject the private hosts custom endpoints exist for.
    private fun String?.validateAsUrl(): AgentConfigError? {
        val value = this?.trim() ?: return null
        if (value.isEmpty()) return null

        val hasScheme = value.startsWith("http://") || value.startsWith("https://")
        val host = value.substringAfter("://", missingDelimiterValue = "").substringBefore('/')
        return if (hasScheme && host.isNotBlank()) null else AgentConfigError.MalformedBaseUrl(value)
    }
}
