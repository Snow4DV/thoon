package com.mvlog.agentconfig.di

import com.mvlog.navigation.screen.ScreenFactory

internal class AgentConfigurationComponentImpl(
    dependencies: AgentConfigurationComponentDependencies = AgentConfigurationComponentDependencies.Impl(),
    private val module: AgentConfigurationModule = AgentConfigurationModule.Impl(dependencies),
) : AgentConfigurationComponent {

    override fun screenFactory(): ScreenFactory = module.screenFactory
}
