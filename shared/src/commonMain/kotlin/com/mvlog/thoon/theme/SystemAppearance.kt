package com.mvlog.thoon.theme

import androidx.compose.runtime.Composable
import com.mvlog.usersettings.api.model.ThemeMode

@Composable
internal expect fun ApplySystemAppearance(themeMode: ThemeMode, isDark: Boolean)
