package com.mvlog.agent.impl.domain.entity

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import kotlin.time.Instant

internal data class ChatMetadata(
    val id: ChatId,
    val title: String?,
    /** Per-chat configuration override; null means follow the app-wide default. */
    val configId: AgentConfigId?,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** When someone last said something. Null until the first turn commits. */
    val lastMessageAt: Instant?,
    val lastMessagePreview: String?,
)

/**
 * One message that matched a search, and the chat it is in.
 *
 * A chat matching several times yields several of these. [messageSequence] says which turn, so a
 * result can open at it; both it and [snippet] are null on the blank-query path, where every chat is
 * listed and nothing in particular matched.
 */
internal data class ChatMetadataMatch(
    val chat: ChatMetadata,
    val snippet: String?,
    val messageSequence: Long?,
)
