package com.mvlog.agent.api.engine

/**
 * A module's contribution of on-device engines.
 */
fun interface LocalEngineProvider {

    fun engines(): List<LocalEngine>
}
