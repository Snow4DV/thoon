package com.mvlog.settings.di

import com.mvlog.navigation.screen.ScreenFactory

internal class SettingsComponentImpl(
    dependencies: SettingsComponentDependencies = SettingsComponentDependencies.Impl(),
    private val module: SettingsModule = SettingsModule.Impl(dependencies),
) : SettingsComponent {

    override fun screenFactory(): ScreenFactory = module.screenFactory
}
