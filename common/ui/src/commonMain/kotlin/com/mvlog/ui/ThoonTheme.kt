package com.mvlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composables.ui.theme.ComposablesTheme
import com.composables.ui.theme.backgroundColor
import com.composables.ui.theme.colors
import com.composeunstyled.theme.Theme

@Composable
fun ThoonTheme(
    content: @Composable () -> Unit,
) {
    ComposablesTheme {
        content()
    }
}


@Composable
fun ThoonPreview(
    content: @Composable () -> Unit,
) {
    ThoonTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Theme[colors][backgroundColor]),
        ) {
            content()
        }
    }
}
