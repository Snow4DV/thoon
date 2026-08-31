package com.mvlog.agent.impl.koog

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.ollama.client.OllamaParams
import ai.koog.prompt.executor.ollama.tools.json.OllamaToolDescriptorSchemaGenerator
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.LLMChoice
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Works around Koog's Ollama client dropping tools when it streams.
 *
 * **Delete this file when Koog forwards tools on its streaming path.** `OllamaClient.execute`
 * builds its request with `tools = ollamaTools`, but `OllamaClient.executeStreaming` (1.1.1
 * `OllamaClient.kt:303`, unchanged in 1.2.0) omits the field entirely — so a streaming run tells the
 * model about no tools at all and it simply answers without them. Nothing errors; the tools are
 * just never mentioned.
 *
 * The only channel the delegate does forward is `params.additionalProperties`, which
 * `OllamaChatRequestDTOSerializer` (an `AdditionalPropertiesFlatteningSerializer`) merges into the
 * request root, skipping any key already present. On the streaming path `tools` is absent, so the
 * injected one survives; on the non-streaming path the real field wins and nothing is sent twice.
 *
 * A decorator, following Koog's own `RetryingLLMClient`, so the workaround stays at the client
 * boundary: `KoogTarget`, the runner, the tool registry and the loop never learn that one provider
 * needs special handling.
 */
internal class ToolForwardingOllamaClient(
    private val delegate: LLMClient,
) : LLMClient() {

    private val schemaGenerator = OllamaToolDescriptorSchemaGenerator()

    override fun llmProvider(): LLMProvider = delegate.llmProvider()

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Flow<StreamFrame> = delegate.executeStreaming(prompt.withTools(tools), model, tools)

    // Everything below is plain delegation: only the streaming request is broken.

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant = delegate.execute(prompt, model, tools)

    override suspend fun executeMultipleChoices(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): LLMChoice = delegate.executeMultipleChoices(prompt, model, tools)

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        delegate.moderate(prompt, model)

    override suspend fun models(): List<LLModel> = delegate.models()

    override fun close() = delegate.close()

    /**
     * Restates [tools] in Ollama's request shape under a key the delegate will pass through.
     *
     * Reuses the delegate's own schema generator rather than hand-rolling JSON Schema, so the
     * parameters the model sees are exactly the ones it would have seen had the field not been
     * dropped.
     */
    private fun Prompt.withTools(tools: List<ToolDescriptor>): Prompt {
        if (tools.isEmpty()) return this

        val encoded = JsonArray(tools.map { it.toOllamaTool() })
        val existing = params.additionalProperties.orEmpty()
        val merged: Map<String, kotlinx.serialization.json.JsonElement> = existing + (TOOLS_KEY to encoded)

        // OllamaParams carries `think`, which the factory sets; copying through the existing params
        // where possible keeps that intact rather than silently resetting it.
        val newParams = when (val current = params) {
            is OllamaParams -> current.copy(additionalProperties = merged)
            else -> OllamaParams(
                temperature = current.temperature,
                maxTokens = current.maxTokens,
                numberOfChoices = current.numberOfChoices,
                speculation = current.speculation,
                schema = current.schema,
                toolChoice = current.toolChoice,
                user = current.user,
                additionalProperties = merged,
            )
        }

        return withParams(newParams)
    }

    private fun ToolDescriptor.toOllamaTool(): JsonObject = buildJsonObject {
        put("type", "function")
        put(
            "function",
            buildJsonObject {
                put("name", name)
                put("description", description)
                put("parameters", schemaGenerator.generate(this@toOllamaTool))
            },
        )
    }

    private companion object {
        const val TOOLS_KEY = "tools"
    }
}
