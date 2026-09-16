package com.mvlog.agent.impl.fake

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.domain.repository.StreamingEntryKind
import com.mvlog.agent.impl.execution.AgentExecutionContext
import com.mvlog.agent.impl.execution.AgentRunner
import com.mvlog.agent.impl.execution.AgentRunnerFactory
import kotlinx.coroutines.delay

internal class EchoAgentRunner(
    private val chatRepository: ChatRepository,
) : AgentRunner {

    override suspend fun run(context: AgentExecutionContext) {
        val entryId = chatRepository.beginStreamingEntry(
            chatId = context.chatId,
            runId = context.runId,
            kind = StreamingEntryKind.Assistant,
        )

        val response = "You said: ${context.prompt}"
        val builder = StringBuilder()

        response.chunked(CHUNK_SIZE).forEach { chunk ->
            delay(CHUNK_DELAY_MS)
            builder.append(chunk)
            chatRepository.updateStreamingText(entryId, builder.toString())
        }

        chatRepository.completeStreamingEntry(entryId, response)
    }

    private companion object {
        const val CHUNK_SIZE = 4
        const val CHUNK_DELAY_MS = 40L
    }
}

internal class EchoAgentRunnerFactory(
    private val chatRepository: ChatRepository,
) : AgentRunnerFactory {

    override fun create(config: AgentConfig?): AgentRunner = EchoAgentRunner(chatRepository)
}
