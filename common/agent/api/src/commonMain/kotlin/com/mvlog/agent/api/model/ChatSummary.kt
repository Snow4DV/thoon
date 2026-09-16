package com.mvlog.agent.api.model

import kotlin.time.Instant

/** A list row; carries no conversation. [title] is null until something names the chat. */
data class ChatSummary(
    val id: ChatId,
    val title: String?,
    /** Override; null follows the default. */
    val configId: AgentConfigId?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /**
     * Commit time of the last turn; null before the first. Not [updatedAt]: that moves on any write
     * (e.g. a model change) and would reorder the list.
     */
    val lastMessageAt: Instant?,
    /** Last message text, for the row. */
    val lastMessagePreview: String?,
    /** Live run state, not metadata: true while a run for this chat is queued or executing. */
    val isWorking: Boolean,
)
