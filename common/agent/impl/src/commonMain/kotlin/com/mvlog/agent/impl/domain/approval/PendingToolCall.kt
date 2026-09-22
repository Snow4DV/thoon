package com.mvlog.agent.impl.domain.approval

/** A tool call the model issued that has no result yet. */
internal data class PendingToolCall(
    val key: String,
    val id: String?,
    val name: String,
    val argumentsJson: String,
)

/** [ordinal] counts only the tool calls of the message, so entries and parts agree on it. */
internal fun toolCallKey(id: String?, ordinal: Int): String = id ?: "call-$ordinal"
