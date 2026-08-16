package com.mvlog.agent.impl.execution

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId

/**
 * Performs one agent turn, reporting everything it produces through the repositories.
 *
 * This is the seam that keeps the agent framework out of the rest of the module: the executor,
 * coordinator and use cases are written against this interface, so the framework-backed
 * implementation can be swapped, stubbed, or tested without touching them.
 */
internal interface AgentRunner {

    suspend fun run(context: AgentExecutionContext)
}

internal data class AgentExecutionContext(
    val chatId: ChatId,
    val runId: AgentRunId,
    val prompt: String,
)
