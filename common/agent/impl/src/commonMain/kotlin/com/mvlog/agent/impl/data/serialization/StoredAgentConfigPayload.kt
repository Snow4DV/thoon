package com.mvlog.agent.impl.data.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Protocol-specific half of a stored configuration, kept as JSON in one column.
 *
 * Mirrors the chat payload approach: the discriminator is also written to its own
 * `kind` column so configurations can be queried by protocol without parsing every row.
 */
@Serializable
internal sealed interface StoredAgentConfigPayload {

    @Serializable
    @SerialName("openai_compatible")
    data class OpenAiCompatible(
        val baseUrl: String? = null,
        val apiKey: String,
        val chatCompletionsPath: String? = null,
    ) : StoredAgentConfigPayload

    @Serializable
    @SerialName("anthropic")
    data class Anthropic(
        val baseUrl: String? = null,
        val apiKey: String,
    ) : StoredAgentConfigPayload

    @Serializable
    @SerialName("local")
    data class Local(
        val engineId: String,
    ) : StoredAgentConfigPayload
}

/** Persisted protocol discriminator, mirrored into `agent_config.kind`. */
internal object AgentConfigKind {
    const val OPENAI_COMPATIBLE = "openai_compatible"
    const val ANTHROPIC = "anthropic"
    const val LOCAL = "local"
}
