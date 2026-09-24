package com.mvlog.usersettings.api.di

import com.mvlog.usersettings.api.usecase.ObserveThemeModeUseCase
import com.mvlog.usersettings.api.usecase.SetThemeModeUseCase

interface UserSettingsComponent {
    fun observeThemeModeUseCase(): ObserveThemeModeUseCase
    fun setThemeModeUseCase(): SetThemeModeUseCase
}
