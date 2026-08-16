package com.mvlog.agent.impl.domain.entity

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import kotlin.time.Instant

internal data class AgentRun(
    val id: AgentRunId,
    val chatId: ChatId,
    val prompt: String,
    /** Recorded when the run starts; null until then, and for runs that never resolved a config. */
    val configId: AgentConfigId?,
    val status: AgentRunStatus,
    val createdAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val error: String?,
)
