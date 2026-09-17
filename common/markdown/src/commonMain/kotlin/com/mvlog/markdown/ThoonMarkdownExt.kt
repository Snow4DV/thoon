package com.mvlog.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownConfig

@Composable
fun ThoonMarkdown(
    content: String,
    isOnPanel: Boolean,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
) {
    val uriHandler = LocalUriHandler.current
    Markdown(
        markdown = content,
        modifier = modifier,
        theme = thoonMarkdownTheme(isOnPanel),
        config = MarkdownConfig.LlmStreaming,
        isStreaming = isStreaming,
        enableScroll = false,
        enableSelection = true,
        onLinkClick = uriHandler::openUri,
    )
}
