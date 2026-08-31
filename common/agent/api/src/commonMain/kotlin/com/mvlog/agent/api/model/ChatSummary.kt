package com.mvlog.agent.api.model

import kotlin.time.Instant

/**
 * A chat as it appears in a list, without its conversation.
 *
 * Deliberately carries no messages: a list showing a preview of the last reply would have to decode
 * every chat's stored history to render one screen. Opening a chat is what loads its content.
 *
 * [title] is null until something names the chat, so a list needs a fallback label of its own.
 */
data class ChatSummary(
    val id: ChatId,
    val title: String?,
    /** Per-chat configuration override; null means the chat follows the app-wide default. */
    val configId: AgentConfigId?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /**
     * When someone last said something, as opposed to when the row last changed.
     *
     * Null until the first turn commits. Distinct from [updatedAt] on purpose: that moves whenever
     * anything about the chat is written, so choosing a different model for it would otherwise
     * reorder the list.
     */
    val lastMessageAt: Instant?,
    /** The last thing said, so a list can be rendered without reading any conversation. */
    val lastMessagePreview: String?,
)
