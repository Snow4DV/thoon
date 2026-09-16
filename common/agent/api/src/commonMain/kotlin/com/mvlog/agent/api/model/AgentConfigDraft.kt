package com.mvlog.agent.api.model

/** What a user supplies for an [AgentConfig]; one shape for create and update. */
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
        val baseUrl: String,
    ) : AgentConfigDraft

    data class Local(
        override val name: String,
        override val modelId: String,
        val engineId: String,
    ) : AgentConfigDraft
}
