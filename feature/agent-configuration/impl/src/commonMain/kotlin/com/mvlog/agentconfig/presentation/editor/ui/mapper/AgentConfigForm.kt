package com.mvlog.agentconfig.presentation.editor.ui.mapper

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft

internal enum class ConfigKind { OpenAiCompatible, Anthropic, Ollama, Local }

internal fun ConfigKind.title(): String = when (this) {
    ConfigKind.OpenAiCompatible -> "OpenAI-compatible"
    ConfigKind.Anthropic -> "Anthropic"
    ConfigKind.Ollama -> "Ollama"
    ConfigKind.Local -> "On-device"
}

/** One flat form for every protocol, so switching protocol keeps what was already typed. */
internal data class AgentConfigForm(
    val kind: ConfigKind = ConfigKind.OpenAiCompatible,
    val name: String = "",
    val modelId: String = "",
    val apiKey: String = "",
    val baseUrl: String = "",
    val chatCompletionsPath: String = "",
    val engineId: String = "",
) {

    /** The rest are irrelevant to [kind], not optional. */
    val visibleFields: List<ConfigField>
        get() = when (kind) {
            ConfigKind.OpenAiCompatible ->
                listOf(ConfigField.Name, ConfigField.ModelId, ConfigField.ApiKey, ConfigField.BaseUrl)

            ConfigKind.Anthropic ->
                listOf(ConfigField.Name, ConfigField.ModelId, ConfigField.ApiKey, ConfigField.BaseUrl)

            ConfigKind.Ollama ->
                listOf(ConfigField.Name, ConfigField.ModelId, ConfigField.BaseUrl)

            ConfigKind.Local ->
                listOf(ConfigField.Name, ConfigField.ModelId, ConfigField.Engine)
        }
}

internal fun AgentConfigForm.read(field: ConfigField): String = when (field) {
    ConfigField.Name -> name
    ConfigField.ModelId -> modelId
    ConfigField.ApiKey -> apiKey
    ConfigField.BaseUrl -> baseUrl
    ConfigField.Engine -> engineId
}

/** Touches only [field]; the protocol in particular is left alone. */
internal fun AgentConfigForm.write(field: ConfigField, value: String): AgentConfigForm = when (field) {
    ConfigField.Name -> copy(name = value)
    ConfigField.ModelId -> copy(modelId = value)
    ConfigField.ApiKey -> copy(apiKey = value)
    ConfigField.BaseUrl -> copy(baseUrl = value)
    ConfigField.Engine -> copy(engineId = value)
}

/**
 * Scheme by host, not by protocol: a private or loopback host is plain HTTP, anything routable is
 * HTTPS. Choosing by protocol would break a local OpenAI-compatible server.
 */
internal fun normalizeBaseUrl(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return trimmed
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed

    val host = trimmed.substringBefore('/').substringBefore(':').lowercase()
    return if (host.isPrivateHost()) "http://$trimmed" else "https://$trimmed"
}

private fun String.isPrivateHost(): Boolean {
    if (this == "localhost" || this == "::1" || endsWith(".local")) return true

    val octets = split(".").mapNotNull { it.toIntOrNull() }
    if (octets.size != 4) return false

    return when (octets[0]) {
        10, 127 -> true
        192 -> octets[1] == 168
        172 -> octets[1] in 16..31
        else -> false
    }
}

/** Fields the chosen protocol does not have are dropped here, never stored. */
internal fun AgentConfigForm.toDraft(): AgentConfigDraft = when (kind) {
    ConfigKind.OpenAiCompatible -> AgentConfigDraft.OpenAiCompatible(
        name = name,
        modelId = modelId,
        apiKey = apiKey,
        // Blank means "provider default"; "" would read as set.
        baseUrl = normalizeBaseUrl(baseUrl).ifBlank { null },
        chatCompletionsPath = chatCompletionsPath.ifBlank { null },
    )

    ConfigKind.Anthropic -> AgentConfigDraft.Anthropic(
        name = name,
        modelId = modelId,
        apiKey = apiKey,
        // Blank means "provider default"; "" would read as set.
        baseUrl = normalizeBaseUrl(baseUrl).ifBlank { null },
    )

    ConfigKind.Ollama -> AgentConfigDraft.Ollama(
        name = name,
        modelId = modelId,
        baseUrl = normalizeBaseUrl(baseUrl),
    )

    ConfigKind.Local -> AgentConfigDraft.Local(
        name = name,
        modelId = modelId,
        engineId = engineId,
    )
}

internal fun AgentConfig.toForm(): AgentConfigForm = when (this) {
    is AgentConfig.OpenAiCompatible -> AgentConfigForm(
        kind = ConfigKind.OpenAiCompatible,
        name = name,
        modelId = modelId,
        apiKey = apiKey,
        baseUrl = baseUrl.orEmpty(),
        chatCompletionsPath = chatCompletionsPath.orEmpty(),
    )

    is AgentConfig.Anthropic -> AgentConfigForm(
        kind = ConfigKind.Anthropic,
        name = name,
        modelId = modelId,
        apiKey = apiKey,
        baseUrl = baseUrl.orEmpty(),
    )

    is AgentConfig.Ollama -> AgentConfigForm(
        kind = ConfigKind.Ollama,
        name = name,
        modelId = modelId,
        baseUrl = baseUrl,
    )

    is AgentConfig.Local -> AgentConfigForm(
        kind = ConfigKind.Local,
        name = name,
        modelId = modelId,
        engineId = engineId,
    )
}
