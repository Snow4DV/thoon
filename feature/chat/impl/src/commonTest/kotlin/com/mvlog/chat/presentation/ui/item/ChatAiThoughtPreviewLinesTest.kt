package com.mvlog.chat.presentation.ui.item

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChatAiThoughtPreviewLinesTest {

    private val thoughts = listOf("first", "second", "third", "fourth")

    @Test
    fun previewShowsTheMostRecentLines() {
        assertEquals(
            listOf("second", "third", "fourth"),
            thoughtPreviewLines(thoughts, isThinking = true, isExpanded = false),
        )
    }

    @Test
    fun blankLinesAreDroppedBeforeTheTailIsTaken() {
        // Filtering after taking would spend preview slots on nothing and show fewer than three.
        assertEquals(
            listOf("a", "b", "c"),
            thoughtPreviewLines(
                listOf("a", "b", "  ", "c"),
                isThinking = true,
                isExpanded = false,
            ),
        )
    }

    @Test
    fun aFinishedOrExpandedThoughtKeepsItsLabel() {
        assertTrue(
            thoughtPreviewLines(thoughts, isThinking = false, isExpanded = false).isEmpty(),
            "a stale tail would sit where the duration belongs",
        )
        assertTrue(
            thoughtPreviewLines(thoughts, isThinking = true, isExpanded = true).isEmpty(),
            "expanded already lists every line, so a preview would duplicate them",
        )
    }
}
