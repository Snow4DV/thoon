package com.mvlog.agent.impl

import com.mvlog.agent.impl.domain.entity.escapeLike
import com.mvlog.agent.impl.domain.entity.snippetAround
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A search result has to show *why* it is a result. Taking the head of a long reply does not — the
 * match can be five hundred characters down and never appear, which is the bug these pin.
 */
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
        // The window has to be pulled back from the end rather than centred past it.
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
        // The end is measured from the match, not from the window, or a long query would be clipped
        // by the very snippet meant to show it.
        val query = "q".repeat(120)

        assertTrue(snippetAround("lead $query trail", query).contains(query))
    }

    @Test
    fun whitespaceIsCollapsedBeforeTheMatchIsLocated() {
        // Folding afterwards would shift every offset measured against the unfolded text.
        val snippet = snippetAround("first\n\n   second   target", "target")

        assertEquals("first second target", snippet)
    }

    @Test
    fun caseFoldsBeyondAscii() {
        // The whole reason `textLower` is written in Kotlin: SQLite folds ASCII only.
        assertTrue(snippetAround("здесь Привет мир", "привет").contains("Привет"))
    }

    @Test
    fun anUnfindableMatchFallsBackToTheHead() {
        // The database matched on a fold this locator cannot reproduce. A head is a poor snippet;
        // a crash or an empty row is worse.
        val snippet = snippetAround("a".repeat(300), "nothing here")

        assertTrue(snippet.endsWith("…"))
        assertTrue(snippet.length < 300)
    }

    @Test
    fun likeWildcardsAreNeutralised() {
        // Unescaped, a query of "50%" matches every message in the database.
        assertEquals("50\\%", escapeLike("50%"))
        assertEquals("a\\_b", escapeLike("a_b"))
        // The backslash goes first, or escaping the others would double-escape it.
        assertEquals("\\\\", escapeLike("\\"))
    }
}
