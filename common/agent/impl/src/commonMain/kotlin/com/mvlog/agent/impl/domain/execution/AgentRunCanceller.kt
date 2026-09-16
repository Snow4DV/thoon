package com.mvlog.agent.impl.domain.execution

import com.mvlog.agent.api.model.AgentRunId

/** Interrupts a live run's coroutine. Implemented by the coordinator, which owns the jobs. */
internal interface AgentRunCanceller {

    suspend fun cancelRunning(runId: AgentRunId): Boolean
}
