package com.mvlog.markdown

import androidx.compose.runtime.Composable
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.controlColor
import com.composables.ui.theme.onBackgroundColor
import com.composables.ui.theme.onPanelColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.MarkdownColors

@Composable
fun thoonMarkdownColors(): MarkdownColors = DefaultMarkdownColors(
    text = Theme[colors][onBackgroundColor],
    codeBackground = Theme[colors][panelColor],
    inlineCodeBackground = Theme[colors][secondaryColor],
    dividerColor = Theme[colors][borderColor],
    tableBackground = Theme[colors][panelColor],
)

@Composable
fun thoonPanelMarkdownColors(): MarkdownColors = DefaultMarkdownColors(
    text = Theme[colors][onPanelColor],
    codeBackground = Theme[colors][secondaryColor],
    inlineCodeBackground = Theme[colors][controlColor],
    dividerColor = Theme[colors][borderColor],
    tableBackground = Theme[colors][secondaryColor],
)
