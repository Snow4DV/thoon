package com.mvlog.chatslist.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.mvlog.chatslist.presentation.ui.highlight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHighlightTest {

    @Test
    fun everyOccurrenceIsStyled() {
        val result = highlight("a cat and another cat", "cat", STYLE)

        assertEquals("a cat and another cat", result.text, "the text itself must not change")
        assertEquals(2, result.spanStyles.size)
        assertTrue(result.spanStyles.all { it.item == STYLE })
    }

    @Test
    fun matchingIgnoresCase() {
        val result = highlight("Snapdragon and snapdragon", "SNAPDRAGON", STYLE)

        assertEquals(2, result.spanStyles.size)
    }

    @Test
    fun aspanCoversExactlyTheMatch() {
        val span = highlight("find the target here", "target", STYLE).spanStyles.single()

        assertEquals(9, span.start)
        assertEquals(15, span.end)
    }

    @Test
    fun ablankQueryStylesNothing() {
        assertEquals(0, highlight("nothing to mark", "", STYLE).spanStyles.size)
        assertEquals(0, highlight("nothing to mark", "   ", STYLE).spanStyles.size)
    }

    @Test
    fun anAbsentQueryStylesNothing() {
        assertEquals(0, highlight("nothing to mark", "absent", STYLE).spanStyles.size)
    }

    @Test
    fun anEmptyQueryDoesNotLoopForever() {
        assertEquals("text", highlight("text", "", STYLE).text)
    }

    private companion object {
        val STYLE = SpanStyle(background = Color.Yellow)
    }
}
