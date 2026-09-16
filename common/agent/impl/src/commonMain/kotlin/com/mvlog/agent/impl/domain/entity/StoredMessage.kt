package com.mvlog.agent.impl.domain.entity

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
