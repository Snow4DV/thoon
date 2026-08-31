package com.mvlog.agent.impl.domain.entity

/**
 * How much of an incoming conversation has to be written.
 *
 * The agent framework hands over the *entire* conversation on every commit, so writing all of it
 * back would rewrite every row to add the two turns that are actually new. Appending is only sound
 * while a conversation grows at its end — which is true today because no history preprocessor is
 * installed, but a windowing one would start dropping leading turns, and appending onto that would
 * leave storage holding a conversation that never happened.
 *
 * So the incoming turns are checked against the turn storage ends on before the count is trusted.
 */
internal sealed interface ConversationDiff {

    /** Everything from [fromSequence] onward is new; earlier turns are already stored. */
    data class Append(val fromSequence: Long) : ConversationDiff

    /** The incoming conversation no longer extends what is stored. Replace it wholesale. */
    data object Rewrite : ConversationDiff
}

/**
 * @param storedCount how many turns storage holds.
 * @param boundary the last turn storage holds, or null when it holds none.
 */
internal fun conversationDiff(
    incoming: List<StoredMessage>,
    storedCount: Int,
    boundary: StoredMessageBoundary?,
): ConversationDiff {
    if (storedCount == 0 || boundary == null) return ConversationDiff.Append(0L)

    // Shorter than what is stored means turns were dropped, not added.
    if (incoming.size < storedCount) return ConversationDiff.Rewrite

    // The turn at the boundary must still be the same turn. A conversation that merely happens to
    // be long enough is not the same as one that extends this one.
    val atBoundary = incoming.getOrNull(boundary.sequence.toInt()) ?: return ConversationDiff.Rewrite
    val extends = atBoundary.id == boundary.id && atBoundary.role == boundary.role

    return if (extends) ConversationDiff.Append(storedCount.toLong()) else ConversationDiff.Rewrite
}

/** The identity of the last stored turn — enough to tell whether a conversation still extends it. */
internal data class StoredMessageBoundary(
    val id: String,
    val sequence: Long,
    val role: StoredRole,
)
