package com.mvlog.agenttools.di

import com.mvlog.agent.tool.AgentToolProvider

/**
 * What this feature offers: tools, and nothing else.
 *
 * No screens — it contributes capability rather than UI, so it publishes one provider and stops.
 */
interface AgentToolsComponent {

    fun agentToolProvider(): AgentToolProvider
}
