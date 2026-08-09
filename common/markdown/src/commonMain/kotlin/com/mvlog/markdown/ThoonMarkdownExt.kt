package com.mvlog.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.compose.Markdown

@Composable
fun ThoonMarkdown(content: String, isOnPanel: Boolean, modifier: Modifier = Modifier) {
    Markdown(
        content = content,
        colors = if (isOnPanel) ThoonPanelMarkdownColors else ThoonMarkdownColors,
        typography = ThoonMarkdownTypography(),
        modifier = modifier,
    )
}
