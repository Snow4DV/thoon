package com.mvlog.agent.tool

import kotlinx.serialization.json.JsonObject

/**
 * Something the model can do.
 *
 * Kept free of the agent framework for the same reason `AgentRunner` is: the framework-facing
 * adapter lives in one place, so a feature contributing a tool never compiles against Koog and a
 * framework change cannot reach across the app.
 */
interface ThoonAgentTool {

    val spec: AgentToolSpec

    /**
     * Runs the tool and returns the text the model reads back.
     *
     * The return value is the tool's whole output — there is no separate success channel — so it
     * should say what actually happened rather than "ok". Throwing is how a failure is reported:
     * the message reaches the model as the result, which is what lets it correct itself and try
     * again, so write it for that reader.
     */
    suspend fun execute(context: ChatToolContext, arguments: JsonObject): String
}
