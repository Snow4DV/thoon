package com.mvlog.chat.presentation

import com.mvlog.agent.api.model.ChatItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HighlightResolutionTest {

    @Test
    fun themessageWinsOverTheReasoningItSharesAturnWith() {
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
        assertEquals("thought", resolveHighlightedItemId(listOf(reasoning("thought", 7L)), 7L))
    }

    @Test
    fun asequenceNothingCarriesResolvesToNothing() {
        assertNull(resolveHighlightedItemId(listOf(assistant("reply", 4L)), 99L))
    }

    @Test
    fun noSequenceMeansNoHighlight() {
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
