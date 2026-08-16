package com.mvlog.agent.api.model

/**
 * A saved connection profile: which model answers, over which protocol, at which endpoint.
 *
 * Variants differ by protocol rather than by vendor, because one protocol covers many vendors —
 * [OpenAiCompatible] serves OpenAI, OpenRouter, Groq, Together, LM Studio, vLLM, llama.cpp and
 * Ollama's `/v1` shim alike.
 */
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

    /**
     * On-device inference. Reserved: no engine exists yet, and selecting one fails the run with an
     * explicit message rather than silently falling back.
     *
     * Note this is not how you reach a locally *hosted* server — Ollama on localhost is an
     * [OpenAiCompatible] config pointing at `http://localhost:11434/v1`.
     */
    data class Local(
        override val id: AgentConfigId,
        override val name: String,
        override val modelId: String,
        val engineId: String,
    ) : AgentConfig
}
