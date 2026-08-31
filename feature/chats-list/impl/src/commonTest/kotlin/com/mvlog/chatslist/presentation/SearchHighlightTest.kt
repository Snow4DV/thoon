package com.mvlog.chatslist.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.mvlog.chatslist.presentation.ui.highlight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A windowed snippet puts the match on the line; this is what says which words it was.
 */
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
        // The search matched case-insensitively; marking only exact case would leave hits unmarked.
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
        // The chats tab renders the same rows with no query at all.
        assertEquals(0, highlight("nothing to mark", "", STYLE).spanStyles.size)
        assertEquals(0, highlight("nothing to mark", "   ", STYLE).spanStyles.size)
    }

    @Test
    fun anAbsentQueryStylesNothing() {
        // A row can match on its title while its subtitle contains the query nowhere.
        assertEquals(0, highlight("nothing to mark", "absent", STYLE).spanStyles.size)
    }

    @Test
    fun anEmptyQueryDoesNotLoopForever() {
        // An empty needle is found at every index without advancing the cursor.
        assertEquals("text", highlight("text", "", STYLE).text)
    }

    private companion object {
        val STYLE = SpanStyle(background = Color.Yellow)
    }
}
