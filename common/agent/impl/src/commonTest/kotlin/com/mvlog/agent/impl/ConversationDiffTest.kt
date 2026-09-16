package com.mvlog.agent.impl

import com.mvlog.agent.impl.domain.entity.ConversationDiff
import com.mvlog.agent.impl.domain.entity.StoredMessage
import com.mvlog.agent.impl.domain.entity.StoredMessageBoundary
import com.mvlog.agent.impl.domain.entity.StoredRole
import com.mvlog.agent.impl.domain.entity.conversationDiff
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationDiffTest {

    @Test
    fun anEmptyStoreTakesEverything() {
        assertEquals(
            ConversationDiff.Append(0L),
            conversationDiff(incoming = conversation(3), storedCount = 0, boundary = null),
        )
    }

    @Test
    fun aConversationThatGrewAppendsOnlyTheNewTurns() {
        assertEquals(
            ConversationDiff.Append(2L),
            conversationDiff(
                incoming = conversation(4),
                storedCount = 2,
                boundary = boundary(1),
            ),
        )
    }

    @Test
    fun nothingNewStillAppendsNothing() {
        assertEquals(
            ConversationDiff.Append(2L),
            conversationDiff(incoming = conversation(2), storedCount = 2, boundary = boundary(1)),
        )
    }

    @Test
    fun aShorterConversationRewrites() {
        // What a windowing preprocessor would hand over.
        assertEquals(
            ConversationDiff.Rewrite,
            conversationDiff(incoming = conversation(1), storedCount = 3, boundary = boundary(2)),
        )
    }

    @Test
    fun aDifferentConversationOfTheSameLengthRewrites() {
        assertEquals(
            ConversationDiff.Rewrite,
            conversationDiff(
                incoming = conversation(4, prefix = "other"),
                storedCount = 2,
                boundary = boundary(1),
            ),
        )
    }

    @Test
    fun aBoundaryPastTheIncomingConversationRewrites() {
        assertEquals(
            ConversationDiff.Rewrite,
            conversationDiff(incoming = conversation(2), storedCount = 2, boundary = boundary(9)),
        )
    }

    private fun conversation(size: Int, prefix: String = "m") = (0 until size).map { index ->
        StoredMessage(
            id = "$prefix-$index",
            sequence = index.toLong(),
            role = if (index % 2 == 0) StoredRole.User else StoredRole.Assistant,
            metaInfoJson = "{}",
            parts = emptyList(),
        )
    }

    private fun boundary(sequence: Int) = StoredMessageBoundary(
        id = "m-$sequence",
        sequence = sequence.toLong(),
        role = if (sequence % 2 == 0) StoredRole.User else StoredRole.Assistant,
    )
}
