package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.AgentRun
import kotlinx.coroutines.flow.Flow

internal interface AgentRunRepository {

    fun observeRuns(chatId: ChatId): Flow<List<AgentRun>>

    /** Drives the coordinator: storage is the work queue, this is its notification channel. */
    fun observeChatsWithPendingRuns(): Flow<Set<ChatId>>

    /** Carries the prompt so the worker needs no reread. */
    suspend fun enqueue(runId: AgentRunId, chatId: ChatId, prompt: String)

    suspend fun get(runId: AgentRunId): AgentRun?

    suspend fun nextQueuedRun(chatId: ChatId): AgentRun?

    suspend fun markRunning(runId: AgentRunId, configId: AgentConfigId?)

    suspend fun markCompleted(runId: AgentRunId)

    suspend fun markFailed(runId: AgentRunId, error: String?)

    suspend fun markCancelled(runId: AgentRunId)

}
