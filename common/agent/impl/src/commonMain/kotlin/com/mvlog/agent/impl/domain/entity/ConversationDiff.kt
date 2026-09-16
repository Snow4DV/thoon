package com.mvlog.agent.impl.domain.entity

internal sealed interface ConversationDiff {

    data class Append(val fromSequence: Long) : ConversationDiff

    data object Rewrite : ConversationDiff
}

internal fun conversationDiff(
    incoming: List<StoredMessage>,
    storedCount: Int,
    boundary: StoredMessageBoundary?,
): ConversationDiff {
    if (storedCount == 0 || boundary == null) return ConversationDiff.Append(0L)

    if (incoming.size < storedCount) return ConversationDiff.Rewrite

    // Same length is not the same conversation: the boundary turn must match by id and role.
    val atBoundary = incoming.getOrNull(boundary.sequence.toInt()) ?: return ConversationDiff.Rewrite
    val extends = atBoundary.id == boundary.id && atBoundary.role == boundary.role

    return if (extends) ConversationDiff.Append(storedCount.toLong()) else ConversationDiff.Rewrite
}

internal data class StoredMessageBoundary(
    val id: String,
    val sequence: Long,
    val role: StoredRole,
)
