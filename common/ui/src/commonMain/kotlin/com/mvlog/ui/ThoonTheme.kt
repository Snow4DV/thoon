package com.mvlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.composables.ui.theme.ColorScheme
import com.composables.ui.theme.ComposablesTheme
import com.composables.ui.theme.LocalColorScheme
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.onBackgroundColor
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.theme.Theme

@Composable
fun ThoonTheme(
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColorScheme provides if (isDark) ColorScheme.Dark else ColorScheme.Light,
    ) {
        ComposablesTheme {
            ProvideContentColor(Theme[colors][onBackgroundColor]) {
                content()
            }
        }
    }
}

@Composable
fun ThoonPreview(
    isDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    ThoonTheme(isDark = isDark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme[colors][backgroundColor]),
        ) {
            content()
        }
    }
}
