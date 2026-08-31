package com.mvlog.agent.api.model

/**
 * The editable shape of an [AgentConfig] — everything a user supplies, and nothing the system owns.
 *
 * Used for both create and update so that "which fields can be edited" is stated exactly once.
 */
sealed interface AgentConfigDraft {

    val name: String
    val modelId: String

    data class OpenAiCompatible(
        override val name: String,
        override val modelId: String,
        val baseUrl: String? = null,
        val apiKey: String,
        val chatCompletionsPath: String? = null,
    ) : AgentConfigDraft

    data class Anthropic(
        override val name: String,
        override val modelId: String,
        val baseUrl: String? = null,
        val apiKey: String,
    ) : AgentConfigDraft

    data class Ollama(
        override val name: String,
        override val modelId: String,
        /** Required, unlike the other variants: see [AgentConfig.Ollama]. */
        val baseUrl: String,
    ) : AgentConfigDraft

    data class Local(
        override val name: String,
        override val modelId: String,
        val engineId: String,
    ) : AgentConfigDraft
}
