package com.mvlog.agent.tool

/** Invoked per run, never at startup. */
fun interface AgentToolProvider {

    fun tools(): List<ThoonAgentTool>
}
