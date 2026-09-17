package com.mvlog.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.controlColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.onBackgroundColor
import com.composables.ui.theme.onPanelColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.hrm.markdown.renderer.MarkdownTheme

/** Markdown-only scale, deliberately larger than ThoonTypography.body. */
private val BASE = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)

private val HEADING_SCALES = listOf(2f, 1.75f, 1.5f, 1.25f, 1.1f, 1f)

private val CODE_BLOCK_RADIUS = 10.dp

/**
 * Starts from the library's light/dark defaults so anything not set here (math, admonitions, task
 * boxes) still follows the system theme, then applies the app tokens on top.
 */
@Composable
fun thoonMarkdownTheme(isOnPanel: Boolean): MarkdownTheme {
    val text = Theme[colors][if (isOnPanel) onPanelColor else onBackgroundColor]
    val codeBackground = Theme[colors][if (isOnPanel) secondaryColor else panelColor]
    val inlineCodeBackground = Theme[colors][if (isOnPanel) controlColor else secondaryColor]
    val border = Theme[colors][borderColor]
    val body = BASE.copy(color = text)

    return MarkdownTheme.auto().copy(
        bodyStyle = body,
        headingStyles = HEADING_SCALES.map { scale ->
            body.copy(fontSize = BASE.fontSize * scale, fontWeight = FontWeight.Bold)
        },
        inlineCodeStyle = SpanStyle(fontFamily = FontFamily.Monospace, fontSize = BASE.fontSize),
        inlineCodeBackground = inlineCodeBackground,
        codeBlockStyle = body.copy(fontFamily = FontFamily.Monospace),
        codeBlockBackground = codeBackground,
        codeBlockCornerRadius = CODE_BLOCK_RADIUS,
        blockQuoteBorderColor = border,
        blockQuoteTextColor = Theme[colors][mutedColor],
        dividerColor = border,
        linkColor = Theme[colors][primaryColor],
        listBulletColor = text,
        tableBorderColor = border,
        tableHeaderBackground = codeBackground,
    )
}
