package com.mvlog.settings.di

import com.mvlog.navigation.screen.ScreenFactory

internal interface SettingsComponent {

    fun screenFactory(): ScreenFactory
}
