package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.AgentRun
import kotlinx.coroutines.flow.Flow

/**
 * Repository that contains statuses of agents
 */
internal interface AgentRunRepository {

    fun observeRuns(chatId: ChatId): Flow<List<AgentRun>>

    /** Drives the coordinator: storage is the work queue, this is its notification channel. */
    fun observeChatsWithPendingRuns(): Flow<Set<ChatId>>

    /** Queues a run for [chatId]. The prompt is carried so a worker can serve it without a reread. */
    suspend fun enqueue(runId: AgentRunId, chatId: ChatId, prompt: String)

    suspend fun get(runId: AgentRunId): AgentRun?

    suspend fun nextQueuedRun(chatId: ChatId): AgentRun?

    /** Records [configId] as the configuration that served the run, alongside the status. */
    suspend fun markRunning(runId: AgentRunId, configId: AgentConfigId?)

    suspend fun markCompleted(runId: AgentRunId)

    suspend fun markFailed(runId: AgentRunId, error: String?)

    suspend fun markCancelled(runId: AgentRunId)

}
