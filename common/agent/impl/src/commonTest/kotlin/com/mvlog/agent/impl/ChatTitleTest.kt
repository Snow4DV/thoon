package com.mvlog.agent.impl

import com.mvlog.agent.impl.domain.usecase.chatTitleFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A chat is named after the first thing asked of it.
 *
 * Until this existed nothing ever named a chat, so every row in the list read `Chat 8424ee48` and a
 * search over titles had nothing to match.
 */
class ChatTitleTest {

    @Test
    fun aShortPromptBecomesTheTitleUnchanged() {
        assertEquals("what is the fastest route?", chatTitleFrom("what is the fastest route?"))
    }

    @Test
    fun onlyTheFirstLineIsUsed() {
        // Prompts are often a paragraph; a list row is one line.
        assertEquals("Summarise this", chatTitleFrom("Summarise this\n\nHere is a long document…"))
    }

    @Test
    fun leadingBlankLinesAreSkipped() {
        assertEquals("the real first line", chatTitleFrom("\n\n   \nthe real first line"))
    }

    @Test
    fun aLongLineIsTruncatedWithAnEllipsis() {
        val title = chatTitleFrom("word ".repeat(50))!!

        assertTrue(title.length <= 61, "was ${title.length}")
        assertTrue(title.endsWith("…"))
    }

    @Test
    fun aBlankPromptNamesNothing() {
        // Storing an empty title would replace the id-derived fallback with nothing at all.
        assertNull(chatTitleFrom("   \n  \n "))
        assertNull(chatTitleFrom(""))
    }
}
