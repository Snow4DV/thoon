package com.mvlog.usersettings.impl.di

import com.mvlog.usersettings.api.usecase.ObserveThemeModeUseCase
import com.mvlog.usersettings.api.usecase.SetThemeModeUseCase
import com.mvlog.usersettings.impl.data.ThemeModeRepository

internal interface UserSettingsModule {
    val observeThemeModeUseCase: ObserveThemeModeUseCase
    val setThemeModeUseCase: SetThemeModeUseCase

    class Impl(dependencies: UserSettingsComponentDependencies) :
        UserSettingsModule, UserSettingsComponentDependencies by dependencies {

        // Singleton: the theme and the settings screen must observe the same flow.
        private val themeModeRepository by lazy { ThemeModeRepository(keyValueStore) }

        override val observeThemeModeUseCase: ObserveThemeModeUseCase
            get() = ObserveThemeModeUseCase { themeModeRepository.themeMode }

        override val setThemeModeUseCase: SetThemeModeUseCase
            get() = SetThemeModeUseCase(themeModeRepository::set)
    }
}
