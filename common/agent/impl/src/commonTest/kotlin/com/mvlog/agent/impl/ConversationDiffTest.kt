package com.mvlog.agent.impl

import com.mvlog.agent.impl.domain.entity.ConversationDiff
import com.mvlog.agent.impl.domain.entity.StoredMessage
import com.mvlog.agent.impl.domain.entity.StoredMessageBoundary
import com.mvlog.agent.impl.domain.entity.StoredRole
import com.mvlog.agent.impl.domain.entity.conversationDiff
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Whether a commit may append or must rewrite.
 *
 * The framework hands over the whole conversation every turn, so appending is the difference between
 * writing two rows and rewriting every row a chat has ever had. It is only sound while a
 * conversation grows at its end — and the failure when it is not is silent, so the check that
 * detects it is worth pinning.
 */
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
        // What a windowing history preprocessor would produce. Appending onto this would leave
        // storage holding turns the conversation no longer contains.
        assertEquals(
            ConversationDiff.Rewrite,
            conversationDiff(incoming = conversation(1), storedCount = 3, boundary = boundary(2)),
        )
    }

    @Test
    fun aDifferentConversationOfTheSameLengthRewrites() {
        // Long enough is not the same as extending: the turn at the boundary is a different turn.
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
