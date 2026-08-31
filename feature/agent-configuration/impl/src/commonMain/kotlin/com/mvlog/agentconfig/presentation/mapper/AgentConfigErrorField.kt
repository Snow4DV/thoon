package com.mvlog.agentconfig.presentation.mapper

import com.mvlog.agent.api.model.AgentConfigError

/** Which input a validation failure belongs under. */
internal enum class ConfigField { Name, ModelId, ApiKey, BaseUrl, Engine }

/**
 * Where a rejection should be shown, and what it should say.
 *
 * Validation is already typed (`ValidateAgentConfigDraftUseCase` returns an [AgentConfigError], not
 * a string), so the only job left is putting the message under the field that caused it. A banner
 * saying "invalid" would throw that away and leave the user hunting.
 *
 * A null field means the failure is not about any one input.
 */
internal fun AgentConfigError.field(): ConfigField? = when (this) {
    AgentConfigError.BlankName -> ConfigField.Name
    AgentConfigError.BlankModelId -> ConfigField.ModelId
    AgentConfigError.MissingApiKey -> ConfigField.ApiKey
    AgentConfigError.MissingBaseUrl -> ConfigField.BaseUrl
    is AgentConfigError.MalformedBaseUrl -> ConfigField.BaseUrl
    AgentConfigError.BlankEngineId -> ConfigField.Engine
    is AgentConfigError.NotFound -> null
}

internal fun AgentConfigError.message(): String = when (this) {
    AgentConfigError.BlankName -> "Give this configuration a name."
    AgentConfigError.BlankModelId -> "A model id is required."
    AgentConfigError.MissingApiKey -> "An API key is required."
    AgentConfigError.MissingBaseUrl ->
        "An address is required. On an emulator the host machine is 10.0.2.2, not localhost."
    // Says what is missing rather than only that something is: "not valid" leaves the reader
    // guessing at a string that looks perfectly reasonable to them.
    is AgentConfigError.MalformedBaseUrl ->
        "'$value' is not a valid address. It should look like http://10.0.2.2:11434."
    AgentConfigError.BlankEngineId -> "Choose an on-device engine."
    is AgentConfigError.NotFound -> "This configuration no longer exists."
}
