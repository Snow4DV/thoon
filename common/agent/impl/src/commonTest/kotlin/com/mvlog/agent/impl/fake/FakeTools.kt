package com.mvlog.agent.impl.fake

import com.mvlog.agent.tool.AgentToolParameter
import com.mvlog.agent.tool.AgentToolSpec
import com.mvlog.agent.tool.AgentToolsCollector
import com.mvlog.agent.tool.ChatToolContext
import com.mvlog.agent.tool.ThoonAgentTool
import kotlinx.serialization.json.JsonObject

/** Records every execution; `q` is the one parameter, optionally approved per value. */
internal class RecordingTool(
    name: String,
    requiresApproval: Boolean,
    approveQueryPerValue: Boolean = false,
) : ThoonAgentTool {

    val calls = mutableListOf<JsonObject>()

    override val spec = AgentToolSpec(
        name = name,
        description = "A test tool.",
        parameters = listOf(
            AgentToolParameter(
                name = "q",
                description = "What to look for.",
                isRequired = false,
                requiresApprovalPerValue = approveQueryPerValue,
            ),
        ),
        requiresApproval = requiresApproval,
    )

    override suspend fun execute(context: ChatToolContext, arguments: JsonObject): String {
        calls += arguments
        return "${spec.name} ran"
    }
}

/** The collector is global: register in a test and reset it in `finally`. */
internal fun registerTools(vararg tools: ThoonAgentTool) {
    AgentToolsCollector.reset()
    AgentToolsCollector.collect { tools.toList() }
}
