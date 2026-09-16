package com.mvlog.agenttools.di

import com.mvlog.agent.tool.AgentToolsCollector
import com.mvlog.init.BaseInitializer

class AgentToolsInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        AgentToolsComponentHolder.set { AgentToolsComponentImpl() }

        AgentToolsCollector.collect { AgentToolsComponentHolder.get().agentToolProvider().tools() }
    }

    private companion object {
        const val TAG = "AgentTools"
    }
}
