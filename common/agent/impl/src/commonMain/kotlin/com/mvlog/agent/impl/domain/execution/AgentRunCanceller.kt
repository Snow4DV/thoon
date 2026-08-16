package com.mvlog.agent.impl.domain.execution

import com.mvlog.agent.api.model.AgentRunId

/**
 * Ability to stop a run that is already executing.
 *
 * Separate from the repositories because cancelling a live run means interrupting a coroutine, not
 * writing a row. Implemented by the coordinator, which owns the running jobs.
 */
internal interface AgentRunCanceller {

    /** Returns true if a live run was found and interrupted. */
    suspend fun cancelRunning(runId: AgentRunId): Boolean
}
