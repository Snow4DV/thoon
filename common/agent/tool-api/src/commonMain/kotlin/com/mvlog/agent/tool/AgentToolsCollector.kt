package com.mvlog.agent.tool

/** Not drained: read on every run. */
object AgentToolsCollector {

    private val providers = mutableListOf<AgentToolProvider>()

    fun collect(provider: AgentToolProvider) {
        providers.add(provider)
    }

    fun collected(): List<AgentToolProvider> = providers.toList()

    /** Tests only. */
    fun reset() {
        providers.clear()
    }
}
