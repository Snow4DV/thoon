package com.mvlog.agent.api.engine

/**
 * Which on-device engines exist, if any.
 *
 * Collect from a base initializer, as with the app's other collectors.
 *
 * **Nothing registers an engine today, so this is empty** — which is the point of having it rather
 * than a free-text field. `AgentConfigDraft.Local` takes an `engineId`, and the client factory
 * rejects every value of it, so a text box there would be one in which every entry is wrong. A
 * configuration screen can offer this list instead, and say plainly that it is empty.
 *
 * As with the other collectors, [obtain] does not drain: it is read every time the editor opens.
 */
object LocalEnginesCollector {

    private val providers = mutableListOf<LocalEngineProvider>()

    /**
     * Should only be invoked in base initializers!
     */
    fun collect(provider: LocalEngineProvider) {
        providers.add(provider)
    }

    fun obtain(): List<LocalEngine> = providers.flatMap { it.engines() }

    /**
     * Only for testing — a process registers its engines once, at startup.
     */
    fun reset() {
        providers.clear()
    }
}
