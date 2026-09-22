package com.mvlog.agent.impl

import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.data.memory.InMemoryAgentStore
import com.mvlog.agent.impl.data.memory.InMemoryChatRepository
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.entity.ToolCallStatus
import com.mvlog.agent.impl.execution.AgentExecutionContext
import com.mvlog.agent.impl.koog.KoogAgentEventSink
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class KoogAgentEventSinkTest {

    private val chatId = ChatId("chat")
    private val repository = InMemoryChatRepository(InMemoryAgentStore(), AgentClock.System, IdGenerator.Random)
    private val sink = KoogAgentEventSink(
        chatRepository = repository,
        context = AgentExecutionContext(chatId = chatId, runId = AgentRunId("run"), prompt = ""),
    )

    @Test
    fun startingACallThatWasHydratedReusesItsEntry() = runTest {
        repository.hydrate(chatId, listOf(toolCall(id = "e1", toolCallId = "c1", status = ToolCallStatus.Running)))

        sink.onToolCallStarting(toolCallId = "c1", name = "search", arguments = "{}")

        val calls = repository.observeEntries(chatId).first().filterIsInstance<ChatEntry.ToolCall>()
        assertEquals(1, calls.size, "a resumed call must not appear twice, was: $calls")
        assertEquals(ToolCallStatus.Running, calls.single().status)
    }

    @Test
    fun startingACallWhoseIdAlreadyFinishedAddsANewEntry() = runTest {
        repository.hydrate(
            chatId,
            listOf(toolCall(id = "e1", toolCallId = "c1", status = ToolCallStatus.Completed("done"))),
        )

        sink.onToolCallStarting(toolCallId = "c1", name = "search", arguments = "{}")

        val calls = repository.observeEntries(chatId).first().filterIsInstance<ChatEntry.ToolCall>()
        assertEquals(2, calls.size, "a finished call is history; a provider reusing its id is a new call")
    }

    private fun toolCall(id: String, toolCallId: String, status: ToolCallStatus) = ChatEntry.ToolCall(
        id = id,
        chatId = chatId,
        runId = null,
        sequence = 1,
        messageSequence = 1,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now(),
        toolCallId = toolCallId,
        name = "search",
        arguments = "{}",
        status = status,
    )
}
