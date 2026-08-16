package com.mvlog.agent.impl.domain.usecase

import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigError

/**
 * Rejects drafts that could not produce a working client, before anything is stored.
 *
 * Validation lives here rather than in the api module so the rules are enforced for every caller,
 * not just ones that remember to check.
 */
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

            // No key: an on-device engine has no endpoint to authenticate against.
            is AgentConfigDraft.Local ->
                if (draft.engineId.isBlank()) AgentConfigError.BlankEngineId else null
        }
    }

    /**
     * A deliberately shallow check — scheme plus a host. Anything stricter would reject valid
     * private hosts (`http://192.168.1.5:1234`, `http://localhost:11434/v1`) that are exactly the
     * reason custom endpoints exist.
     */
    private fun String?.validateAsUrl(): AgentConfigError? {
        val value = this?.trim() ?: return null
        if (value.isEmpty()) return null

        val hasScheme = value.startsWith("http://") || value.startsWith("https://")
        val host = value.substringAfter("://", missingDelimiterValue = "").substringBefore('/')
        return if (hasScheme && host.isNotBlank()) null else AgentConfigError.MalformedBaseUrl(value)
    }
}
