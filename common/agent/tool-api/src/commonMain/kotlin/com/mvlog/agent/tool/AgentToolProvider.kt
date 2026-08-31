package com.mvlog.agent.tool

/**
 * A feature's contribution of tools.
 *
 * A provider rather than a finished list so construction stays lazy: initializers run at startup,
 * and building a tool there would drag its dependencies — a file system, an HTTP client — into
 * process launch for a conversation that may never happen.
 */
fun interface AgentToolProvider {

    fun tools(): List<ThoonAgentTool>
}
