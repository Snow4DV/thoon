package com.mvlog.agenttools.di

import com.mvlog.agent.tool.AgentToolsCollector
import com.mvlog.init.BaseInitializer

/**
 * Registers this feature's tools with the agent.
 *
 * Collecting a provider rather than built tools keeps startup free of the filesystem and the HTTP
 * client — the provider is not invoked until a run actually needs a registry.
 */
class AgentToolsInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        AgentToolsComponentHolder.set { AgentToolsComponentImpl() }

        AgentToolsCollector.collect { AgentToolsComponentHolder.get().agentToolProvider().tools() }
    }

    private companion object {
        const val TAG = "AgentTools"
    }
}
