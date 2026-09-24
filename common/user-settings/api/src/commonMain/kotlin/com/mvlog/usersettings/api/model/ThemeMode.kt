package com.mvlog.usersettings.api.model

enum class ThemeMode {
    System,
    Light,
    Dark,
}

fun ThemeMode.isDark(systemIsDark: Boolean): Boolean = when (this) {
    ThemeMode.System -> systemIsDark
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}
