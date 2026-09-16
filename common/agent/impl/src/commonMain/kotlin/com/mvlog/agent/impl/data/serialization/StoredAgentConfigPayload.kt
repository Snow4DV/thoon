package com.mvlog.agent.impl.data.serialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("ollama")
    data class Ollama(
        val baseUrl: String,
    ) : StoredAgentConfigPayload

    @Serializable
    @SerialName("local")
    data class Local(
        val engineId: String,
    ) : StoredAgentConfigPayload
}

/** Must equal the @SerialName values above. */
internal object AgentConfigKind {
    const val OPENAI_COMPATIBLE = "openai_compatible"
    const val ANTHROPIC = "anthropic"
    const val OLLAMA = "ollama"
    const val LOCAL = "local"
}
