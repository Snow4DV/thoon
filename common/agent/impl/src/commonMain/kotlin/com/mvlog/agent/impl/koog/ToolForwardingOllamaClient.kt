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
 * Koog's OllamaClient.executeStreaming omits tools from the request; re-injected via
 * params.additionalProperties, the one field the delegate flattens into the request root. Delete
 * when fixed upstream.
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
     * Uses Koog's own OllamaToolDescriptorSchemaGenerator, so the schema matches the non-streaming
     * path.
     */
    private fun Prompt.withTools(tools: List<ToolDescriptor>): Prompt {
        if (tools.isEmpty()) return this

        val encoded = JsonArray(tools.map { it.toOllamaTool() })
        val existing = params.additionalProperties.orEmpty()
        val merged: Map<String, kotlinx.serialization.json.JsonElement> = existing + (TOOLS_KEY to encoded)

        // copy when already OllamaParams, or think set by the factory is lost.
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
