package com.mvlog.agent.impl.domain.approval

import com.mvlog.agent.tool.AgentToolSpec
import com.mvlog.agent.tool.AgentToolsCollector
import com.mvlog.log.TLogger

internal fun interface ToolSpecCatalog {

    fun specs(): List<AgentToolSpec>

    object Collected : ToolSpecCatalog {

        override fun specs(): List<AgentToolSpec> = AgentToolsCollector.collected()
            .flatMap { provider ->
                runCatching { provider.tools() }
                    .onFailure { TLogger.e(TAG, "A tool provider failed to build its tools", it) }
                    .getOrDefault(emptyList())
            }
            .map { it.spec }

        private const val TAG = "AgentTools"
    }
}

internal fun ToolSpecCatalog.gatedToolNames(): List<String> =
    specs().filter { it.requiresApproval }.map { it.name }
