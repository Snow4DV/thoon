package com.mvlog.usersettings.impl.di

import com.mvlog.usersettings.api.di.UserSettingsComponent
import com.mvlog.usersettings.api.usecase.ObserveThemeModeUseCase
import com.mvlog.usersettings.api.usecase.SetThemeModeUseCase

internal class UserSettingsComponentImpl(
    dependencies: UserSettingsComponentDependencies = UserSettingsComponentDependencies.Impl(),
    private val module: UserSettingsModule = UserSettingsModule.Impl(dependencies),
) : UserSettingsComponent {

    override fun observeThemeModeUseCase(): ObserveThemeModeUseCase = module.observeThemeModeUseCase

    override fun setThemeModeUseCase(): SetThemeModeUseCase = module.setThemeModeUseCase
}
