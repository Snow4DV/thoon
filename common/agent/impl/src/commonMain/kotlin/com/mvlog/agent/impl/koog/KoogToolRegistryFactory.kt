package com.mvlog.agent.impl.koog

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.serialization.JSONSerializer
import ai.koog.serialization.typeToken
import com.mvlog.agent.tool.AgentToolParameterType
import com.mvlog.agent.tool.AgentToolSpec
import com.mvlog.agent.tool.AgentToolsCollector
import com.mvlog.agent.tool.ChatToolContext
import com.mvlog.agent.tool.ThoonAgentTool
import com.mvlog.log.TLogger
import kotlinx.serialization.json.JsonObject

/**
 * Turns what features registered into the registry the agent framework understands.
 *
 * This is the only place the two vocabularies meet. Descriptors are written by hand rather than
 * derived by reflection because Koog's reflective tool API (`ToolSet`, `@Tool`, `asTools()`) lives
 * in `jvmCommonMain` and is absent from the iOS klib — using it would compile on Android and break
 * the iOS build, the same trap as `openAIClient()`.
 */
internal class KoogToolRegistryFactory {

    /**
     * Builds the registry for one chat.
     *
     * Per chat rather than per process: a tool is scoped to the conversation it serves, and the
     * chat id is only known once a run starts.
     */
    fun create(chatId: String): ToolRegistry {
        val context = ChatToolContext(chatId)
        val tools = AgentToolsCollector.collected().flatMap { provider ->
            // One misbehaving provider should cost its own tools, not every other feature's.
            runCatching { provider.tools() }
                .onFailure { TLogger.e(TAG, "A tool provider failed to build its tools", it) }
                .getOrDefault(emptyList())
        }

        TLogger.d(TAG, "Offering ${tools.size} tool(s) to chat $chatId")

        return ToolRegistry {
            tools.forEach { tool -> tool(KoogToolAdapter(tool, context)) }
        }
    }

    private companion object {
        const val TAG = "AgentTools"
    }
}

/**
 * Presents one [ThoonAgentTool] to the framework.
 *
 * The descriptor is passed in rather than generated: `descriptor` is final on `ToolBase`, and the
 * generating constructor would derive a schema from [JsonObject] — which describes "any object" and
 * would tell the model nothing about the arguments this tool actually wants.
 *
 * The result type is `String` because that is what the model reads; [encodeResultToString] is the
 * identity for the same reason.
 */
private class KoogToolAdapter(
    private val tool: ThoonAgentTool,
    private val context: ChatToolContext,
) : Tool<JsonObject, String>(
    argsType = typeToken<JsonObject>(),
    resultType = typeToken<String>(),
    descriptor = tool.spec.toDescriptor(),
) {

    override suspend fun execute(args: JsonObject): String = tool.execute(context, args)

    override fun encodeResultToString(result: String, serializer: JSONSerializer): String = result
}

private fun AgentToolSpec.toDescriptor(): ToolDescriptor = ToolDescriptor(
    name = name,
    description = description,
    requiredParameters = parameters.filter { it.isRequired }.map { it.toDescriptor() },
    optionalParameters = parameters.filterNot { it.isRequired }.map { it.toDescriptor() },
)

private fun com.mvlog.agent.tool.AgentToolParameter.toDescriptor() = ToolParameterDescriptor(
    name = name,
    description = description,
    type = when (type) {
        AgentToolParameterType.String -> ToolParameterType.String
        AgentToolParameterType.Integer -> ToolParameterType.Integer
        AgentToolParameterType.Boolean -> ToolParameterType.Boolean
    },
)
