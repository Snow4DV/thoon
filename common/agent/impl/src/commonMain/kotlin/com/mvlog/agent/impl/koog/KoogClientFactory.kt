package com.mvlog.agent.impl.koog

import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.ktor.KtorKoogHttpClient
import io.ktor.client.HttpClient
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.anthropic.AnthropicClientSettings
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.mvlog.agent.api.model.AgentConfig

/**
 * Turns a saved configuration into the Koog client and model that serve it.
 *
 * Three things here are load-bearing and easy to get wrong:
 * - Koog 1.1.1 has no `SingleLLMPromptExecutor`; [MultiLLMPromptExecutor] with one client is the
 *   equivalent.
 * - The `openAIClient()` / `anthropicClient()` helper functions exist only on JVM and are absent
 *   from the iOS klib, so the constructors are used directly. Calling the helpers from commonMain
 *   compiles on Android and breaks the iOS build.
 * - [LLMCapability.OpenAIEndpoint.Completions] must be declared, otherwise the client targets the
 *   Responses API, which most OpenAI-compatible servers do not implement.
 */
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

        is AgentConfig.Local -> error(
            "Local engine '${config.engineId}' is not supported yet"
        )
    }

    /**
     * Lets the agent framework build its clients on top of the app's shared Ktor client.
     *
     * Deliberately not `HttpClientFactoryResolver.resolve()`: that lives in Koog's `jvmCommonMain`
     * and is absent from the iOS klib, so calling it from here compiles on Android and breaks the
     * iOS build. This route is genuinely multiplatform, and every request ends up sharing one
     * connection pool with the rest of the app.
     *
     * A factory rather than a finished client, because the endpoint and the API key are per
     * configuration — the framework supplies both when it calls `create`.
     */
    private val koogHttpClientFactory: KoogHttpClient.Factory
        get() = KtorKoogHttpClient.Factory(baseClient = httpClient)

    private companion object {
        const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com"
        const val DEFAULT_CHAT_COMPLETIONS_PATH = "v1/chat/completions"
        const val DEFAULT_ANTHROPIC_BASE_URL = "https://api.anthropic.com"

        // Explicit element type: these objects' nearest common supertype is inferred as `Any`,
        // which compiles on Android and fails on Native.
        val OPENAI_CAPABILITIES = listOf<LLMCapability>(
            LLMCapability.Completion,
            LLMCapability.Temperature,
            LLMCapability.Tools,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.OpenAIEndpoint.Completions,
        )

        val ANTHROPIC_CAPABILITIES = listOf<LLMCapability>(
            LLMCapability.Completion,
            LLMCapability.Temperature,
            LLMCapability.Tools,
            LLMCapability.Schema.JSON.Standard,
        )
    }
}

internal data class KoogTarget(
    val executor: PromptExecutor,
    val model: LLModel,
)
