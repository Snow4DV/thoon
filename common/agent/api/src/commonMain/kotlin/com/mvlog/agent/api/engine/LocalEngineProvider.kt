package com.mvlog.agent.api.engine

fun interface LocalEngineProvider {

    fun engines(): List<LocalEngine>
}
