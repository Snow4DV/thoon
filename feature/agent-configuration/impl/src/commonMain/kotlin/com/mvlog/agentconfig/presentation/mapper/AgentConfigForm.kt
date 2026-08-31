package com.mvlog.agentconfig.presentation.mapper

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft

/** The protocols a configuration may use, as the editor offers them. */
internal enum class ConfigKind { OpenAiCompatible, Anthropic, Ollama, Local }

internal fun ConfigKind.title(): String = when (this) {
    ConfigKind.OpenAiCompatible -> "OpenAI-compatible"
    ConfigKind.Anthropic -> "Anthropic"
    ConfigKind.Ollama -> "Ollama"
    ConfigKind.Local -> "On-device"
}

/**
 * Every field the editor can show, regardless of which are relevant.
 *
 * One flat bag rather than a variant per protocol so that switching the picker keeps what was
 * already typed — a name and a model id mean the same thing whichever protocol is selected, and
 * clearing them on every switch would be its own small cruelty.
 */
internal data class AgentConfigForm(
    val kind: ConfigKind = ConfigKind.OpenAiCompatible,
    val name: String = "",
    val modelId: String = "",
    val apiKey: String = "",
    val baseUrl: String = "",
    val chatCompletionsPath: String = "",
    val engineId: String = "",
) {

    /** Which inputs to show for [kind]; the rest are irrelevant, not merely optional. */
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

/** The value currently held for [field]. */
internal fun AgentConfigForm.read(field: ConfigField): String = when (field) {
    ConfigField.Name -> name
    ConfigField.ModelId -> modelId
    ConfigField.ApiKey -> apiKey
    ConfigField.BaseUrl -> baseUrl
    ConfigField.Engine -> engineId
}

/**
 * The form with [field] set to [value] and everything else — the selected protocol above all —
 * left alone.
 */
internal fun AgentConfigForm.write(field: ConfigField, value: String): AgentConfigForm = when (field) {
    ConfigField.Name -> copy(name = value)
    ConfigField.ModelId -> copy(modelId = value)
    ConfigField.ApiKey -> copy(apiKey = value)
    ConfigField.BaseUrl -> copy(baseUrl = value)
    ConfigField.Engine -> copy(engineId = value)
}

/**
 * Supplies the scheme when an address was typed without one.
 *
 * Requiring `http://` by hand is the wrong tax to charge for the commonest thing anyone does here:
 * point the app at a server on their own machine. Typing `10.0.2.2:11434` is what people mean, and
 * rejecting it teaches them nothing.
 *
 * The scheme is chosen by where the host is, not by which protocol is selected. A private or
 * loopback address is plain HTTP — Ollama and LM Studio both serve it that way — while anything
 * routable is HTTPS. Guessing by protocol instead would break a local OpenAI-compatible server,
 * turning a clear validation error into a confusing connection failure.
 *
 * The result is stored, so reopening the configuration shows the scheme that was chosen. Nothing is
 * hidden; the address is only completed.
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

/**
 * The draft this form describes.
 *
 * Each protocol takes only its own fields — an `Ollama` config has no API key and a `Local` one no
 * address — so anything typed under a different protocol and left behind is dropped here rather
 * than smuggled into storage.
 */
internal fun AgentConfigForm.toDraft(): AgentConfigDraft = when (kind) {
    ConfigKind.OpenAiCompatible -> AgentConfigDraft.OpenAiCompatible(
        name = name,
        modelId = modelId,
        apiKey = apiKey,
        baseUrl = normalizeBaseUrl(baseUrl).ifBlank { null },
        chatCompletionsPath = chatCompletionsPath.ifBlank { null },
    )

    ConfigKind.Anthropic -> AgentConfigDraft.Anthropic(
        name = name,
        modelId = modelId,
        apiKey = apiKey,
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

/** Fills the form from a saved configuration, for editing. */
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

/** What a list row says under the name, so protocols are told apart at a glance. */
internal fun AgentConfig.describe(): String = when (this) {
    is AgentConfig.OpenAiCompatible -> "OpenAI-compatible · $modelId"
    is AgentConfig.Anthropic -> "Anthropic · $modelId"
    is AgentConfig.Ollama -> "Ollama · $modelId · $baseUrl"
    is AgentConfig.Local -> "On-device · $modelId"
}
