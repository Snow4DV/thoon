package com.mvlog.usersettings.api.usecase

import com.mvlog.usersettings.api.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** Has a value on first read and never suspends; System until the user picks another. */
fun interface ObserveThemeModeUseCase {
    operator fun invoke(): StateFlow<ThemeMode>
}

fun interface SetThemeModeUseCase {
    operator fun invoke(mode: ThemeMode)
}
