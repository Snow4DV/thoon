package com.mvlog.settings.di

import com.mvlog.settings.api.SettingsScreen
import com.mvlog.settings.api.ChatConfigurationScreen
import com.mvlog.settings.presentation.editor.AgentConfigEditorScreen
import com.mvlog.settings.presentation.list.AgentConfigListScreen
import com.mvlog.init.BaseInitializer
import com.mvlog.navigation.screen.ScreenFactoriesCollector

class SettingsInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        SettingsComponentHolder.set { SettingsComponentImpl() }

        val provider = { SettingsComponentHolder.get().screenFactory() }

        ScreenFactoriesCollector.collect(SettingsScreen.serializer(), provider)
        ScreenFactoriesCollector.collect(AgentConfigListScreen.serializer(), provider)
        ScreenFactoriesCollector.collect(AgentConfigEditorScreen.serializer(), provider)
        ScreenFactoriesCollector.collect(ChatConfigurationScreen.serializer(), provider)
    }

    private companion object {
        const val TAG = "Settings"
    }
}
