package com.mvlog.agent.impl.data.memory

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.AgentRun
import com.mvlog.agent.impl.domain.entity.AgentRunStatus
import com.mvlog.agent.impl.domain.repository.AgentRunRepository
import com.mvlog.agent.impl.util.AgentClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class InMemoryAgentRunRepository(
    private val store: InMemoryAgentStore,
    private val clock: AgentClock,
) : AgentRunRepository {

    override fun observeRuns(chatId: ChatId): Flow<List<AgentRun>> =
        store.runs
            .map { runs -> runs.values.filter { it.chatId == chatId }.sortedBy { it.createdAt } }
            .distinctUntilChanged()

    override fun observeChatsWithPendingRuns(): Flow<Set<ChatId>> =
        store.runs
            .map { runs -> runs.values.filter { it.status.isPending }.map { it.chatId }.toSet() }
            .distinctUntilChanged()

    override fun observeChatsWithActiveRuns(): Flow<Set<ChatId>> =
        store.runs
            .map { runs -> runs.values.filter { !it.status.isTerminal }.map { it.chatId }.toSet() }
            .distinctUntilChanged()

    override suspend fun enqueue(runId: AgentRunId, chatId: ChatId, prompt: String) {
        store.transaction {
            store.putRun(
                AgentRun(
                    id = runId,
                    chatId = chatId,
                    prompt = prompt,
                    configId = null,
                    status = AgentRunStatus.Queued,
                    createdAt = clock.now(),
                    startedAt = null,
                    finishedAt = null,
                    error = null,
                )
            )
        }
    }

    override suspend fun get(runId: AgentRunId): AgentRun? = store.runs.value[runId]

    override suspend fun nextQueuedRun(chatId: ChatId): AgentRun? =
        store.transaction {
            store.runs.value.values
                .filter { it.chatId == chatId && it.status == AgentRunStatus.Queued }
                .minByOrNull { it.createdAt }
        }

    override suspend fun markRunning(runId: AgentRunId, configId: AgentConfigId?) {
        update(runId) {
            it.copy(status = AgentRunStatus.Running, startedAt = clock.now(), configId = configId)
        }
    }

    override suspend fun markCompleted(runId: AgentRunId) {
        finish(runId, AgentRunStatus.Completed, error = null)
    }

    override suspend fun markFailed(runId: AgentRunId, error: String?) {
        finish(runId, AgentRunStatus.Failed, error = error)
    }

    override suspend fun markCancelled(runId: AgentRunId) {
        finish(runId, AgentRunStatus.Cancelled, error = null)
    }

    private suspend fun finish(runId: AgentRunId, status: AgentRunStatus, error: String?) {
        update(runId) { it.copy(status = status, finishedAt = clock.now(), error = error) }
    }

    private suspend fun update(runId: AgentRunId, transform: (AgentRun) -> AgentRun) {
        store.transaction {
            val run = store.runs.value[runId] ?: return@transaction
            store.putRun(transform(run))
        }
    }
}
