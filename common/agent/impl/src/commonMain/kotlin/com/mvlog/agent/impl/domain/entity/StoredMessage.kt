package com.mvlog.agent.impl.domain.entity

/**
 * A conversation turn as storage holds it.
 *
 * Deliberately not an agent-framework `Message`: `ChatHistoryRepository` lives in the domain layer,
 * and letting the framework's types reach it would spread that dependency through storage. Only the
 * codec knows how to turn these into messages and back.
 *
 * [metaInfoJson] and [StoredPart.payloadJson] are opaque strings here for the same reason.
 */
internal data class StoredMessage(
    val id: String,
    val sequence: Long,
    val role: StoredRole,
    val metaInfoJson: String,
    val parts: List<StoredPart>,
)

internal data class StoredPart(
    val id: String,
    val sequence: Long,
    val kind: StoredPartKind,
    /** Text parts only — the one thing a search should ever match. */
    val text: String?,
    val toolName: String?,
    val payloadJson: String,
)

internal enum class StoredRole(val stored: String) {
    System("system"),
    User("user"),
    Assistant("assistant"),
    ;

    companion object {
        fun of(stored: String): StoredRole? = entries.firstOrNull { it.stored == stored }
    }
}

internal enum class StoredPartKind(val stored: String) {
    Text("text"),
    Reasoning("reasoning"),
    ToolCall("tool_call"),
    ToolResult("tool_result"),
    Attachment("attachment"),
    ;

    companion object {
        fun of(stored: String): StoredPartKind? = entries.firstOrNull { it.stored == stored }
    }
}
