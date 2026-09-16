package com.mvlog.agent.impl

import com.mvlog.agent.impl.domain.entity.escapeLike
import com.mvlog.agent.impl.domain.entity.snippetAround
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchSnippetTest {

    @Test
    fun amatchDeepInAMessageIsVisible() {
        val text = "x".repeat(500) + " snapdragon runs hot " + "y".repeat(500)

        val snippet = snippetAround(text, "snapdragon")

        assertTrue(snippet.contains("snapdragon"), "the match must be in the window, was: $snippet")
        assertTrue(snippet.startsWith("…"), "text was cut before the match, so say so: $snippet")
        assertTrue(snippet.endsWith("…"), "text was cut after the match, so say so: $snippet")
    }

    @Test
    fun amatchAtTheEndIsNotCutOff() {
        val snippet = snippetAround("z".repeat(300) + " finalword", "finalword")

        assertTrue(snippet.contains("finalword"), "was: $snippet")
        assertTrue(!snippet.endsWith("…"), "nothing follows the match, so nothing was cut: $snippet")
    }

    @Test
    fun amatchAtTheStartHasNoLeadingEllipsis() {
        val snippet = snippetAround("snapdragon " + "z".repeat(300), "snapdragon")

        assertTrue(snippet.startsWith("snapdragon"), "was: $snippet")
    }

    @Test
    fun ashortMessageIsShownWhole() {
        assertEquals("just this", snippetAround("just this", "this"))
    }

    @Test
    fun amatchLongerThanTheWindowSurvivesIntact() {
        val query = "q".repeat(120)

        assertTrue(snippetAround("lead $query trail", query).contains(query))
    }

    @Test
    fun whitespaceIsCollapsedBeforeTheMatchIsLocated() {
        val snippet = snippetAround("first\n\n   second   target", "target")

        assertEquals("first second target", snippet)
    }

    @Test
    fun caseFoldsBeyondAscii() {
        assertTrue(snippetAround("здесь Привет мир", "привет").contains("Привет"))
    }

    @Test
    fun anUnfindableMatchFallsBackToTheHead() {
        // SQL matched on a fold this locator cannot reproduce; a head beats a crash or empty row.
        val snippet = snippetAround("a".repeat(300), "nothing here")

        assertTrue(snippet.endsWith("…"))
        assertTrue(snippet.length < 300)
    }

    @Test
    fun likeWildcardsAreNeutralised() {
        assertEquals("50\\%", escapeLike("50%"))
        assertEquals("a\\_b", escapeLike("a_b"))
        // The backslash goes first, or escaping the others would double-escape it.
        assertEquals("\\\\", escapeLike("\\"))
    }
}
