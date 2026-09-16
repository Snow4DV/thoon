package com.mvlog.agentconfig.di

import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.agentconfig.api.ChatConfigurationScreen
import com.mvlog.agentconfig.presentation.AgentConfigEditorScreen
import com.mvlog.agentconfig.presentation.AgentConfigListScreen
import com.mvlog.init.BaseInitializer
import com.mvlog.navigation.screen.ScreenFactoriesCollector

/**
 * Wires the configuration feature into the app.
 *
 * Four registrations sharing one provider: the collector is keyed by screen type, and a feature
 * owning several screens registers each. The lambda still runs only on first navigation, so opening
 * the app touches none of this.
 */
class AgentConfigurationInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        AgentConfigurationComponentHolder.set { AgentConfigurationComponentImpl() }

        val provider = { AgentConfigurationComponentHolder.get().screenFactory() }

        ScreenFactoriesCollector.collect(AgentConfigurationScreen.serializer(), provider)
        ScreenFactoriesCollector.collect(AgentConfigListScreen.serializer(), provider)
        ScreenFactoriesCollector.collect(AgentConfigEditorScreen.serializer(), provider)
        ScreenFactoriesCollector.collect(ChatConfigurationScreen.serializer(), provider)
    }

    private companion object {
        const val TAG = "AgentConfiguration"
    }
}
