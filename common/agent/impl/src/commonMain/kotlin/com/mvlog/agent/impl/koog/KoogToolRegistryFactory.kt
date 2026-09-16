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

internal class KoogToolRegistryFactory {

    /** Per chat: tools are sandboxed to one conversation, known only once a run starts. */
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
 * Descriptor passed in: it is final on ToolBase, and the generating constructor would schema
 * JsonObject as "any object".
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
