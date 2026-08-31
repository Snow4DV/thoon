package com.mvlog.chat.presentation

import com.mvlog.agent.api.model.ChatItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Which rendered item a search result meant.
 *
 * One stored turn becomes several items, so the sequence alone does not name one — and the order
 * they project in is not the order that matters here.
 */
@OptIn(ExperimentalTime::class)
class HighlightResolutionTest {

    @Test
    fun themessageWinsOverTheReasoningItSharesAturnWith() {
        // Reasoning projects first, so taking the first match would tint a collapsed thought bubble
        // rather than the text that was actually searched.
        val id = resolveHighlightedItemId(
            items = listOf(reasoning("thought", 4L), assistant("reply", 4L)),
            messageSequence = 4L,
        )

        assertEquals("reply", id)
    }

    @Test
    fun auserPromptIsFound() {
        assertEquals("prompt", resolveHighlightedItemId(listOf(user("prompt", 2L)), 2L))
    }

    @Test
    fun anItemWithNoMessageOfItsOwnStillResolves() {
        // Nothing but reasoning carries this turn. Showing the hit beats ignoring it.
        assertEquals("thought", resolveHighlightedItemId(listOf(reasoning("thought", 7L)), 7L))
    }

    @Test
    fun asequenceNothingCarriesResolvesToNothing() {
        // History compression, or a timeline restored from a checkpoint whose indices moved. The
        // screen opens normally rather than reporting an error at someone.
        assertNull(resolveHighlightedItemId(listOf(assistant("reply", 4L)), 99L))
    }

    @Test
    fun noSequenceMeansNoHighlight() {
        // The ordinary case: the chat was opened from the list, not from a search result.
        assertNull(resolveHighlightedItemId(listOf(assistant("reply", 4L)), null))
    }

    @Test
    fun aliveItemIsNeverATarget() {
        // Entries produced mid-run carry no anchor yet.
        assertNull(resolveHighlightedItemId(listOf(assistant("streaming", null)), 4L))
    }

    private fun user(id: String, sequence: Long?) =
        ChatItem.UserMessage(id = id, messageSequence = sequence, createdAt = NOW, text = "t")

    private fun assistant(id: String, sequence: Long?) = ChatItem.AssistantMessage(
        id = id, messageSequence = sequence, createdAt = NOW, text = "t", isStreaming = false,
    )

    private fun reasoning(id: String, sequence: Long?) = ChatItem.Reasoning(
        id = id, messageSequence = sequence, createdAt = NOW, text = "t", isStreaming = false,
    )

    private companion object {
        val NOW = Clock.System.now()
    }
}
