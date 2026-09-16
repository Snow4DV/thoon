package com.mvlog.agenttools.di

import com.mvlog.agent.tool.AgentToolProvider

interface AgentToolsComponent {

    fun agentToolProvider(): AgentToolProvider
}
