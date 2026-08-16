package com.mvlog.agent.impl.execution

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.execution.AgentRunCanceller
import com.mvlog.agent.impl.domain.repository.AgentRunRepository
import com.mvlog.agent.impl.domain.repository.ConversationRepository
import com.mvlog.agent.impl.util.IdGenerator
import com.mvlog.log.TLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Drains queued runs, one worker per chat, for as long as the component lives.
 *
 * Storage is the work queue: prompts become durable rows, and this observes them. That is what
 * makes a run outlive the screen that started it — nothing here is tied to a presenter's lifetime.
 */
internal class AgentRunCoordinator(
    private val runRepository: AgentRunRepository,
    private val conversationRepository: ConversationRepository,
    private val executor: AgentRunExecutor,
    private val idGenerator: IdGenerator,
    private val scope: CoroutineScope,
) : AgentRunCanceller {

    private val guard = Mutex()

    private val workers = mutableMapOf<ChatId, Job>()

    private val activeRuns = mutableMapOf<AgentRunId, Job>()

    private var started = false

    fun start() {
        scope.launch {
            val alreadyStarted = guard.withLock { started.also { started = true } }
            if (alreadyStarted) return@launch

            requeueUnansweredPrompts()

            runRepository.observeChatsWithPendingRuns().collect { chatIds ->
                chatIds.forEach { ensureWorker(it) }
            }
        }
    }

    /**
     * Re-queues prompts that were accepted but never answered.
     *
     * The run queue lives in memory, so nothing about a previous process survives into this one.
     * What does survive is the conversation: a chat whose durable record ends with an unanswered
     * prompt is a prompt the user is still waiting on, and this is the only thing that will notice.
     *
     * A chat with uncommitted work is left alone — its partial run is shown as it was, and the user
     * decides whether to retry, rather than the app silently re-running something that may have
     * failed for a reason that has not gone away.
     */
    private suspend fun requeueUnansweredPrompts() {
        conversationRepository.chatsWithUnansweredPrompts().forEach { chatId ->
            val prompt = conversationRepository.unansweredPrompt(chatId) ?: return@forEach
            TLogger.i(TAG, "Re-queuing unanswered prompt for ${chatId.value}")
            runRepository.enqueue(
                runId = AgentRunId(idGenerator.newId()),
                chatId = chatId,
                prompt = prompt,
            )
        }
    }

    private suspend fun ensureWorker(chatId: ChatId) {
        guard.withLock {
            val existing = workers[chatId]
            if (existing?.isActive == true) return@withLock

            workers[chatId] = scope.launch { drain(chatId) }
        }
    }

    /**
     * Executes every queued run for one chat, in order, until none remain.
     *
     * Draining in a loop rather than one run per emission means prompts queued while a run is in
     * flight are picked up without waiting for another notification.
     */
    private suspend fun drain(chatId: ChatId) {
        while (true) {
            val run = runRepository.nextQueuedRun(chatId) ?: break
            val job = scope.launch { executor.execute(run) }
            guard.withLock { activeRuns[run.id] = job }
            try {
                job.join()
            } finally {
                guard.withLock { activeRuns.remove(run.id) }
            }
        }
        guard.withLock { workers.remove(chatId) }
    }

    override suspend fun cancelRunning(runId: AgentRunId): Boolean {
        val job = guard.withLock { activeRuns[runId] } ?: return false
        TLogger.d(TAG, "Cancelling run ${runId.value}")
        job.cancel()
        job.join()
        return true
    }

    private companion object {
        const val TAG = "AgentRunCoordinator"
    }
}
