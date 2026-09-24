package com.mvlog.settings.presentation.editor.ui.mapper

import com.mvlog.agent.api.model.AgentConfigError

internal enum class ConfigField { Name, ModelId, ApiKey, BaseUrl, Engine }

/** Null: the failure is not about any one input. */
internal fun AgentConfigError.field(): ConfigField? = when (this) {
    AgentConfigError.BlankName -> ConfigField.Name
    AgentConfigError.BlankModelId -> ConfigField.ModelId
    AgentConfigError.MissingApiKey -> ConfigField.ApiKey
    AgentConfigError.MissingBaseUrl -> ConfigField.BaseUrl
    is AgentConfigError.MalformedBaseUrl -> ConfigField.BaseUrl
    AgentConfigError.BlankEngineId -> ConfigField.Engine
    is AgentConfigError.NotFound -> null // nothing the user typed is wrong
}

internal fun AgentConfigError.message(): String = when (this) {
    AgentConfigError.BlankName -> "Give this configuration a name."
    AgentConfigError.BlankModelId -> "A model id is required."
    AgentConfigError.MissingApiKey -> "An API key is required."
    AgentConfigError.MissingBaseUrl ->
        "An address is required. On an emulator the host machine is 10.0.2.2, not localhost."
    // Echoes the value: "not valid" alone leaves the reader guessing at a string that looks fine.
    is AgentConfigError.MalformedBaseUrl ->
        "'$value' is not a valid address. It should look like http://10.0.2.2:11434."
    AgentConfigError.BlankEngineId -> "Choose an on-device engine."
    is AgentConfigError.NotFound -> "This configuration no longer exists."
}
