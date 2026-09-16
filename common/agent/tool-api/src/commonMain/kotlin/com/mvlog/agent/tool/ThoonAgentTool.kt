package com.mvlog.agent.tool

import kotlinx.serialization.json.JsonObject

interface ThoonAgentTool {

    val spec: AgentToolSpec

    /**
     * Returns the text the model reads — say what happened, not "ok". Throw to fail: the message is
     * delivered to the model as the result.
     */
    suspend fun execute(context: ChatToolContext, arguments: JsonObject): String
}
