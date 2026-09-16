package com.mvlog.agent.impl

import com.mvlog.agent.impl.domain.usecase.chatTitleFrom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChatTitleTest {

    @Test
    fun aShortPromptBecomesTheTitleUnchanged() {
        assertEquals("what is the fastest route?", chatTitleFrom("what is the fastest route?"))
    }

    @Test
    fun onlyTheFirstLineIsUsed() {
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
        assertNull(chatTitleFrom("   \n  \n "))
        assertNull(chatTitleFrom(""))
    }
}
