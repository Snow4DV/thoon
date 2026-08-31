package com.mvlog.agentconfig.di

import com.mvlog.agentconfig.presentation.AgentConfigurationPresenterFactory
import com.mvlog.agentconfig.presentation.AgentConfigurationUiFactory
import com.mvlog.navigation.screen.ScreenFactory

internal interface AgentConfigurationModule {

    val screenFactory: ScreenFactory

    class Impl(
        dependencies: AgentConfigurationComponentDependencies,
    ) : AgentConfigurationModule, AgentConfigurationComponentDependencies by dependencies {

        override val screenFactory: ScreenFactory
            get() = ScreenFactory(
                presenterFactory = AgentConfigurationPresenterFactory(
                    observeConfigs = observeAgentConfigsUseCase,
                    observeConfig = observeAgentConfigUseCase,
                    observeDefaultConfig = observeDefaultAgentConfigUseCase,
                    createConfig = createAgentConfigUseCase,
                    updateConfig = updateAgentConfigUseCase,
                    deleteConfig = deleteAgentConfigUseCase,
                    setDefaultConfig = setDefaultAgentConfigUseCase,
                    observeChatConfig = observeChatConfigUseCase,
                    setChatConfig = setChatConfigUseCase,
                ),
                uiFactory = AgentConfigurationUiFactory(),
            )
    }
}
