package com.mvlog.agentconfig.di

import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.agentconfig.api.ChatConfigurationScreen
import com.mvlog.agentconfig.presentation.editor.AgentConfigEditorScreen
import com.mvlog.agentconfig.presentation.list.AgentConfigListScreen
import com.mvlog.init.BaseInitializer
import com.mvlog.navigation.screen.ScreenFactoriesCollector

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
