package com.mvlog.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.panelColor
import com.composeunstyled.theme.Theme

@Composable
fun Modifier.panel(shape: Shape): Modifier = this
    .clip(shape)
    .background(Theme[colors][panelColor], shape)
    .border(1.dp, Theme[colors][borderColor], shape)
