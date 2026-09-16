package com.mvlog.agent.impl.domain.entity

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import kotlin.time.Instant

internal data class ChatMetadata(
    val id: ChatId,
    val title: String?,
    val configId: AgentConfigId?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastMessageAt: Instant?,
    val lastMessagePreview: String?,
)

/**
 * One per matching message, so a chat may appear more than once. Both nullables are null on the
 * blank-query path.
 */
internal data class ChatMetadataMatch(
    val chat: ChatMetadata,
    val snippet: String?,
    val messageSequence: Long?,
)
