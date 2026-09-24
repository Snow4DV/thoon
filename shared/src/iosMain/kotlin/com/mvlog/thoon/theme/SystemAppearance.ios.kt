package com.mvlog.thoon.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.mvlog.usersettings.api.model.ThemeMode
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

// Unspecified for System, not the resolved style: an override would also pin the trait that
// isSystemInDarkTheme() reads, and System would stop following the device.
@Composable
internal actual fun ApplySystemAppearance(themeMode: ThemeMode, isDark: Boolean) {
    SideEffect {
        val style = when (themeMode) {
            ThemeMode.System -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            ThemeMode.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            ThemeMode.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }
        UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { it.windows.filterIsInstance<UIWindow>() }
            .forEach { it.overrideUserInterfaceStyle = style }
    }
}
