package com.mvlog.settings.di

import com.mvlog.settings.presentation.SettingsPresenterFactory
import com.mvlog.settings.presentation.SettingsUiFactory
import com.mvlog.navigation.screen.ScreenFactory

internal interface SettingsModule {

    val screenFactory: ScreenFactory

    class Impl(
        dependencies: SettingsComponentDependencies,
    ) : SettingsModule, SettingsComponentDependencies by dependencies {

        override val screenFactory: ScreenFactory
            get() = ScreenFactory(
                presenterFactory = SettingsPresenterFactory(
                    observeConfigs = observeAgentConfigsUseCase,
                    getConfig = getAgentConfigUseCase,
                    observeDefaultConfig = observeDefaultAgentConfigUseCase,
                    createConfig = createAgentConfigUseCase,
                    updateConfig = updateAgentConfigUseCase,
                    deleteConfig = deleteAgentConfigUseCase,
                    setDefaultConfig = setDefaultAgentConfigUseCase,
                    observeChatConfigOverride = observeChatConfigOverrideUseCase,
                    setChatConfig = setChatConfigUseCase,
                    observeToolApprovalRules = observeToolApprovalRulesUseCase,
                    addToolApprovalRule = addToolApprovalRuleUseCase,
                    revokeToolApprovalRule = revokeToolApprovalRuleUseCase,
                    getApprovalGatedTools = getApprovalGatedToolsUseCase,
                    localEngineProvider = localEngineProvider,
                    observeThemeMode = observeThemeModeUseCase,
                    setThemeMode = setThemeModeUseCase,
                ),
                uiFactory = SettingsUiFactory(),
            )
    }
}
