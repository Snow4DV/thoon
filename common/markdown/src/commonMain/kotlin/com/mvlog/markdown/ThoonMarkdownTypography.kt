package com.mvlog.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.composables.ui.theme.colors
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownTypography

private val baseTextStyle = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)

@Composable
fun ThoonMarkdownTypography(): MarkdownTypography {
    val base = baseTextStyle
    val linkColor = Theme[colors][primaryColor]

    return DefaultMarkdownTypography(
        h1 = base.copy(fontSize = base.fontSize * 2f, fontWeight = FontWeight.Bold),
        h2 = base.copy(fontSize = base.fontSize * 1.75f, fontWeight = FontWeight.Bold),
        h3 = base.copy(fontSize = base.fontSize * 1.5f, fontWeight = FontWeight.Bold),
        h4 = base.copy(fontSize = base.fontSize * 1.25f, fontWeight = FontWeight.Bold),
        h5 = base.copy(fontSize = base.fontSize * 1.1f, fontWeight = FontWeight.Bold),
        h6 = base.copy(fontWeight = FontWeight.Bold),
        text = base,
        code = base.copy(fontFamily = FontFamily.Monospace),
        inlineCode = base.copy(fontFamily = FontFamily.Monospace),
        quote = base.copy(fontStyle = FontStyle.Italic),
        paragraph = base,
        ordered = base,
        bullet = base,
        list = base,
        table = base,
        textLink = TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline,
            ),
        ),
    )
}
