package com.mvlog.agent.api.model

/**
 * A saved connection profile: which model answers, over which protocol, at which endpoint.
 *
 * Variants differ by protocol rather than by vendor, because one protocol covers many vendors —
 * [OpenAiCompatible] serves OpenAI, OpenRouter, Groq, Together, LM Studio, vLLM and llama.cpp
 * alike. Ollama has its own variant rather than riding that shim, because its `/v1` compatibility
 * layer drops the model's reasoning.
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
     * A locally hosted Ollama server, over its native `/api/chat`.
     *
     * Deliberately not an [OpenAiCompatible] pointed at `/v1`: that shim reports reasoning in a
     * non-standard `reasoning` field which the OpenAI client does not model, so every thought the
     * model has is discarded before it can reach the timeline. The native protocol carries it in a
     * `thinking` field that survives the trip.
     *
     * No API key — Ollama does not authenticate.
     */
    data class Ollama(
        override val id: AgentConfigId,
        override val name: String,
        override val modelId: String,
        /** Required: `localhost` on a phone or emulator is the device itself, not the host. */
        val baseUrl: String,
    ) : AgentConfig

    /**
     * On-device inference. Reserved: no engine exists yet, and selecting one fails the run with an
     * explicit message rather than silently falling back.
     *
     * Note this is not how you reach a locally *hosted* server — a server on the same machine or
     * the same network is an [Ollama] config, or an [OpenAiCompatible] one for anything speaking
     * that protocol.
     */
    data class Local(
        override val id: AgentConfigId,
        override val name: String,
        override val modelId: String,
        val engineId: String,
    ) : AgentConfig
}
