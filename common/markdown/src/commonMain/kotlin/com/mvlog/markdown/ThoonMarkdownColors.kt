package com.mvlog.markdown

import androidx.compose.ui.graphics.Color
import com.composables.ui.theme.borderColor
import com.composables.ui.theme.colors
import com.composables.ui.theme.controlColor
import com.composables.ui.theme.onBackgroundColor
import com.composables.ui.theme.onPanelColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.mikepenz.markdown.model.MarkdownColors

object ThoonMarkdownColors : MarkdownColors {
    override val text: Color
        get() = Theme[colors][onBackgroundColor]

    override val codeBackground: Color
        get() = Theme[colors][panelColor]
    override val inlineCodeBackground: Color
        get() = Theme[colors][secondaryColor]
    override val dividerColor: Color
        get() = Theme[colors][borderColor]
    override val tableBackground: Color
        get() = Theme[colors][panelColor]
}

object ThoonPanelMarkdownColors : MarkdownColors {
    override val text: Color
        get() = Theme[colors][onPanelColor]

    override val codeBackground: Color
        get() = Theme[colors][secondaryColor]
    override val inlineCodeBackground: Color
        get() = Theme[colors][controlColor]
    override val dividerColor: Color
        get() = Theme[colors][borderColor]
    override val tableBackground: Color
        get() = Theme[colors][secondaryColor]
}
