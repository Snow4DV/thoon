package com.mvlog.agent.impl

import com.mvlog.agent.api.model.ChatExecutionState
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatItem
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.impl.di.ThoonAgentComponentImpl
import com.mvlog.agent.impl.fake.TestAgentModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AgentPipelineTest {

    @Test
    fun acceptedPromptIsVisibleBeforeAnythingExecutesIt() = pipelineTest(startRuntime = false) {
        sendPrompt(chatId, "hello")
        advanceUntilIdle()

        val accepted = states.last()
        assertEquals(1, accepted.items.size, "prompt must be persisted immediately")
        assertEquals("hello", assertIs<ChatItem.UserMessage>(accepted.items.single()).text)

        val working = assertIs<ChatExecutionState.Working>(accepted.execution)
        assertEquals(
            ChatExecutionState.Working.Phase.Starting,
            working.phase,
            "the agent must already look busy before execution begins",
        )
    }

    @Test
    fun promptIsAnsweredThroughObservedState() = pipelineTest {
        sendPrompt(chatId, "hello")
        advanceUntilIdle()

        val finished = states.last()
        assertEquals(ChatExecutionState.Idle, finished.execution)
        assertEquals(2, finished.items.size)

        val answer = assertIs<ChatItem.AssistantMessage>(finished.items[1])
        assertEquals("You said: hello", answer.text)
        assertTrue(!answer.isStreaming, "a finished answer must not still be marked streaming")
    }

    @Test
    fun queuedPromptsAreAnsweredInOrder() = pipelineTest {
        sendPrompt(chatId, "first")
        sendPrompt(chatId, "second")
        advanceUntilIdle()

        val texts = states.last().items.map { item ->
            when (item) {
                is ChatItem.UserMessage -> item.text
                is ChatItem.AssistantMessage -> item.text
                else -> error("unexpected item $item")
            }
        }

        // Both accepted before either runs, so both prompts precede both answers.
        assertEquals(listOf("first", "second", "You said: first", "You said: second"), texts)
    }

    @Test
    fun secondPromptIsReportedAsQueuedWhileTheFirstRuns() = pipelineTest {
        sendPrompt(chatId, "first")
        sendPrompt(chatId, "second")
        advanceTimeBy(60)

        val working = assertIs<ChatExecutionState.Working>(states.last().execution)
        assertEquals(1, working.queuedPrompts, "the waiting prompt should be reported as queued")
    }

    @Test
    fun cancellingMidRunSettlesTheStreamingEntry() = pipelineTest {
        val runId = sendPrompt(chatId, "cancel me")
        // Far enough in that the answer has started streaming, well short of completing it.
        advanceTimeBy(60)

        assertTrue(
            states.last().items.any { it is ChatItem.AssistantMessage && it.isStreaming },
            "expected a streaming answer to be in flight before cancelling",
        )

        cancelAgentRun(runId)
        advanceUntilIdle()

        val finished = states.last()
        assertEquals(
            ChatExecutionState.Idle,
            finished.execution,
            "a cancelled run is finished, not failed",
        )
        assertTrue(
            finished.items.none { it is ChatItem.AssistantMessage && it.isStreaming },
            "cancelling must not leave a permanently streaming message",
        )
    }

    private class Fixture(
        private val testScope: TestScope,
        component: ThoonAgentComponentImpl,
        val chatId: ChatId,
        val states: List<ChatState>,
    ) {
        val sendPrompt = component.sendPromptUseCase()
        val cancelAgentRun = component.cancelAgentRunUseCase()

        fun advanceUntilIdle() = testScope.advanceUntilIdle()

        fun advanceTimeBy(milliseconds: Long) = testScope.advanceTimeBy(milliseconds)
    }

    private fun pipelineTest(
        startRuntime: Boolean = true,
        body: suspend Fixture.() -> Unit,
    ): TestResult = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        try {
            val component = ThoonAgentComponentImpl(
                module = TestAgentModule(agentScope = scope, echoRuns = true),
            )
            if (startRuntime) component.startAgentRuntimeUseCase().invoke()

            val chatId = component.createChatUseCase()(configId = null)
            val states = mutableListOf<ChatState>()
            scope.launch { component.observeChatUseCase()(chatId).collect(states::add) }
            advanceUntilIdle()

            Fixture(
                testScope = this,
                component = component,
                chatId = chatId,
                states = states,
            ).body()
        } finally {
            scope.cancel()
        }
    }
}
