package com.mvlog.usersettings.api.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {

    @Test
    fun systemFollowsTheDevice() {
        assertEquals(
            true,
            ThemeMode.System.isDark(systemIsDark = true),
            "System must be dark on a dark device",
        )
        assertEquals(
            false,
            ThemeMode.System.isDark(systemIsDark = false),
            "System must be light on a light device",
        )
    }

    @Test
    fun anExplicitModeIgnoresTheDevice() {
        listOf(true, false).forEach { systemIsDark ->
            assertEquals(
                false,
                ThemeMode.Light.isDark(systemIsDark),
                "Light must stay light whatever the device says",
            )
            assertEquals(
                true,
                ThemeMode.Dark.isDark(systemIsDark),
                "Dark must stay dark whatever the device says",
            )
        }
    }
}
