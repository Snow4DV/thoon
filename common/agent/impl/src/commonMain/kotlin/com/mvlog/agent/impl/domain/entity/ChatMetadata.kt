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
)
