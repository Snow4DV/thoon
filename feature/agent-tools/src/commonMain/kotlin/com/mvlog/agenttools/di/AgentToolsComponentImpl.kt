package com.mvlog.agenttools.di

import com.mvlog.agent.tool.AgentToolProvider

internal class AgentToolsComponentImpl(
    private val module: AgentToolsModule = AgentToolsModule.Impl(
        AgentToolsComponentDependencies.Impl(),
    ),
) : AgentToolsComponent {

    override fun agentToolProvider(): AgentToolProvider = module.agentToolProvider
}
