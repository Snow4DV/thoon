package com.mvlog.agent.api.model

/** A saved connection profile: which model, over which protocol, at which endpoint. */
sealed interface AgentConfig {

    val id: AgentConfigId
    val name: String
    val modelId: String

    data class OpenAiCompatible(
        override val id: AgentConfigId,
        override val name: String,
        override val modelId: String,
        /** Null uses the provider default, `https://api.openai.com`. */
        val baseUrl: String?,
        val apiKey: String,
        /** Null uses `v1/chat/completions`. */
        val chatCompletionsPath: String?,
    ) : AgentConfig

    data class Anthropic(
        override val id: AgentConfigId,
        override val name: String,
        override val modelId: String,
        /** Null uses the provider default, `https://api.anthropic.com`. */
        val baseUrl: String?,
        val apiKey: String,
    ) : AgentConfig

    /** Native `/api/chat`. No API key — Ollama does not authenticate. */
    data class Ollama(
        override val id: AgentConfigId,
        override val name: String,
        override val modelId: String,
        /** Required — there is no provider default. */
        val baseUrl: String,
    ) : AgentConfig

    /**
     * Reserved: no engine exists yet; selecting one fails the run explicitly. A server on the LAN
     * is [Ollama] or [OpenAiCompatible].
     */
    data class Local(
        override val id: AgentConfigId,
        override val name: String,
        override val modelId: String,
        val engineId: String,
    ) : AgentConfig
}
