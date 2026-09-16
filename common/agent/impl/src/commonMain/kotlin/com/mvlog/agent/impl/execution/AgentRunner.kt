package com.mvlog.agent.impl.execution

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId

internal interface AgentRunner {

    suspend fun run(context: AgentExecutionContext)
}

internal data class AgentExecutionContext(
    val chatId: ChatId,
    val runId: AgentRunId,
    val prompt: String,
)
