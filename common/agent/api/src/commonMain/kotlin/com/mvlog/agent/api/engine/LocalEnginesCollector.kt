package com.mvlog.agent.api.engine

/** Empty today: nothing registers an engine. */
object LocalEnginesCollector {

    private val providers = mutableListOf<LocalEngineProvider>()

    fun collect(provider: LocalEngineProvider) {
        providers.add(provider)
    }

    fun obtain(): List<LocalEngine> = providers.flatMap { it.engines() }

    /** Tests only. */
    fun reset() {
        providers.clear()
    }
}
