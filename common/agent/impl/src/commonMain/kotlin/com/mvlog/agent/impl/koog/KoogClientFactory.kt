package com.mvlog.agent.impl.koog

import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.ktor.KtorKoogHttpClient
import io.ktor.client.HttpClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaParams
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.params.LLMParams
import com.mvlog.agent.api.model.AgentConfig

internal class KoogClientFactory(
    private val httpClient: HttpClient,
) {

    fun create(config: AgentConfig): KoogTarget = when (config) {
        is AgentConfig.OpenAiCompatible -> KoogTarget(
            executor = MultiLLMPromptExecutor(
                OpenAILLMClient(
                    apiKey = config.apiKey,
                    settings = OpenAIClientSettings(
                        baseUrl = config.baseUrl ?: DEFAULT_OPENAI_BASE_URL,
                        chatCompletionsPath = config.chatCompletionsPath
                            ?: DEFAULT_CHAT_COMPLETIONS_PATH,
                    ),
                    httpClientFactory = koogHttpClientFactory,
                ),
            ),
            model = LLModel(
                provider = LLMProvider.OpenAI,
                id = config.modelId,
                capabilities = OPENAI_CAPABILITIES,
            ),
        )

        is AgentConfig.Anthropic -> KoogTarget(
            executor = MultiLLMPromptExecutor(
                AnthropicLLMClient(
                    apiKey = config.apiKey,
                    settings = AnthropicClientSettings(
                        baseUrl = config.baseUrl ?: DEFAULT_ANTHROPIC_BASE_URL,
                    ),
                    httpClientFactory = koogHttpClientFactory,
                ),
            ),
            model = LLModel(
                provider = LLMProvider.Anthropic,
                id = config.modelId,
                capabilities = ANTHROPIC_CAPABILITIES,
            ),
        )

        is AgentConfig.Ollama -> KoogTarget(
            executor = MultiLLMPromptExecutor(
                ToolForwardingOllamaClient(
                    OllamaClient(
                        httpClientFactory = koogHttpClientFactory,
                        baseUrl = config.baseUrl,
                    ),
                ),
            ),
            model = LLModel(
                provider = LLMProvider.Ollama,
                id = config.modelId,
                capabilities = OLLAMA_CAPABILITIES,
            ),
            // think = true explicitly: Ollama's per-model default would show reasoning for some
            // models only.
            params = OllamaParams(think = true),
        )

        is AgentConfig.Local -> error(
            "Local engine '${config.engineId}' is not supported yet"
        )
    }

    /**
     * A factory, not a client: the framework supplies the per-config base URL and auth in
     * create.
     */
    private val koogHttpClientFactory: KoogHttpClient.Factory
        get() = KtorKoogHttpClient.Factory(baseClient = httpClient)

    private companion object {
        const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com"
        const val DEFAULT_CHAT_COMPLETIONS_PATH = "v1/chat/completions"
        const val DEFAULT_ANTHROPIC_BASE_URL = "https://api.anthropic.com"

        // <LLMCapability> explicit: Native infers List<Any> here and fails.
        val OPENAI_CAPABILITIES = listOf<LLMCapability>(
            LLMCapability.Completion,
            LLMCapability.Temperature,
            LLMCapability.Tools,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.OpenAIEndpoint.Completions,
        )

        // No OpenAIEndpoint: that capability picks between two OpenAI-only routes.
        val OLLAMA_CAPABILITIES = listOf<LLMCapability>(
            LLMCapability.Completion,
            LLMCapability.Temperature,
            LLMCapability.Tools,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.Thinking,
        )

        val ANTHROPIC_CAPABILITIES = listOf<LLMCapability>(
            LLMCapability.Completion,
            LLMCapability.Temperature,
            LLMCapability.Tools,
            LLMCapability.Schema.JSON.Standard,
        )
    }
}

/** Per-protocol prompt parameters the runner applies; null means provider defaults. */
internal data class KoogTarget(
    val executor: PromptExecutor,
    val model: LLModel,
    val params: LLMParams? = null,
)
