package com.mvlog.agent.impl.execution

import com.mvlog.agent.impl.domain.entity.AgentRun
import com.mvlog.agent.impl.domain.repository.AgentRunRepository
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.domain.usecase.ResolveAgentConfigUseCase
import com.mvlog.log.TLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Every exit — success, failure, cancellation — settles streaming entries, or the timeline shows
 * a run that no longer exists.
 */
internal class AgentRunExecutor(
    private val runnerFactory: AgentRunnerFactory,
    private val resolveConfig: ResolveAgentConfigUseCase,
    private val runRepository: AgentRunRepository,
    private val chatRepository: ChatRepository,
    private val mutexes: ChatRunMutexRegistry,
) {

    suspend fun execute(run: AgentRun) {
        mutexes.withChatLock(run.chatId) { executeLocked(run) }
    }

    private suspend fun executeLocked(run: AgentRun) {
        val config = resolveConfig(run.chatId)
        runRepository.markRunning(run.id, config?.id)

        val runner = try {
            runnerFactory.create(config)
        } catch (error: UnsupportedConfigurationException) {
            settle(run)
            runRepository.markFailed(run.id, error.message)
            return
        }

        try {
            runner.run(
                AgentExecutionContext(
                    chatId = run.chatId,
                    runId = run.id,
                    prompt = run.prompt,
                )
            )
            settle(run)
            runRepository.markCompleted(run.id)
        } catch (cancellation: CancellationException) {
            // Without NonCancellable the status write is skipped and the run stays Running forever.
            withContext(NonCancellable) {
                settle(run)
                runRepository.markCancelled(run.id)
            }
            throw cancellation
        } catch (error: Throwable) {
            TLogger.e(TAG, "Run ${run.id.value} failed", error)
            settle(run)
            runRepository.markFailed(run.id, error.message)
        }
    }

    private suspend fun settle(run: AgentRun) {
        chatRepository.settleStreamingEntries(run.id)
    }

    private companion object {
        const val TAG = "AgentRunExecutor"
    }
}
