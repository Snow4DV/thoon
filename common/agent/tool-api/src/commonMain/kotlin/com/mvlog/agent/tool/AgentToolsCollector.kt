package com.mvlog.agent.tool

/**
 * Where features register what the agent can do.
 *
 * Collect from a base initializer, exactly as with `AppOnCreateActionsCollector`.
 *
 * **One deliberate difference from that collector: [collected] does not clear the list.** Start-up
 * actions run once, so draining them is right. A tool registry is rebuilt for every run, so
 * draining here would give the first run its tools and every run afterwards none — a bug that looks
 * like the model forgetting its abilities partway through a session.
 */
object AgentToolsCollector {

    private val providers = mutableListOf<AgentToolProvider>()

    /**
     * Should only be invoked in base initializers!
     */
    fun collect(provider: AgentToolProvider) {
        providers.add(provider)
    }

    fun collected(): List<AgentToolProvider> = providers.toList()

    /**
     * Only for testing — a process registers its tools once, at startup.
     */
    fun reset() {
        providers.clear()
    }
}
