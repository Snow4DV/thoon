package com.mvlog.agent.impl

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.fake.TestAgentModule
import com.mvlog.agent.impl.fake.TestJson
import com.mvlog.agent.impl.koog.KoogMessageRowCodec
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/** Mostly negative cases: a retry that quietly does nothing looks exactly like one that worked. */
@OptIn(ExperimentalCoroutinesApi::class)
class RetryChatTest {

    @Test
    fun anUnansweredPromptIsRunAgainWithItsOriginalText() = retryTest { f ->
        f.module.conversationRepository.appendUserPrompt(f.chatId, "what failed?")

        assertTrue(f.module.retryChatUseCase(f.chatId), "an unanswered prompt is retryable")

        val queued = f.module.agentRunRepository.nextQueuedRun(f.chatId)
        assertEquals(
            "what failed?",
            queued?.prompt,
            "retry must re-send what the user actually asked, was: $queued",
        )
    }

    @Test
    fun achatWaitingOnNothingIsNotRetried() = retryTest { f ->
        assertTrue(
            !f.module.retryChatUseCase(f.chatId),
            "an idle chat has nothing to retry, and saying otherwise leaves the user waiting",
        )
        assertEquals(null, f.module.agentRunRepository.nextQueuedRun(f.chatId))
    }

    @Test
    fun aninterruptedToolTurnResumesWithoutRepeatingThePrompt() = retryTest { f ->
        val now = Clock.System.now()
        f.commitMessages(
            listOf(
                Message.User(
                    parts = listOf(MessagePart.Text("find my invoices")),
                    metaInfo = RequestMetaInfo(now),
                ),
                Message.Assistant(
                    parts = listOf(MessagePart.Tool.Call(id = "c1", tool = "searchInvoices", args = "{}")),
                    metaInfo = ResponseMetaInfo(now),
                ),
                Message.User(
                    parts = listOf(
                        MessagePart.Tool.Result(
                            id = "c1",
                            tool = "searchInvoices",
                            parts = listOf(MessagePart.Text("three found")),
                        ),
                    ),
                    metaInfo = RequestMetaInfo(now),
                ),
            )
        )

        assertTrue(f.module.retryChatUseCase(f.chatId), "an interrupted tool turn is retryable")

        assertEquals("", f.module.agentRunRepository.nextQueuedRun(f.chatId)?.prompt)
    }

    @Test
    fun retryIsANoOpWhileARunIsStillLive() = retryTest { f ->
        f.module.conversationRepository.appendUserPrompt(f.chatId, "still going")
        f.module.agentRunRepository.enqueue(AgentRunId("live"), f.chatId, "still going")

        assertTrue(
            !f.module.retryChatUseCase(f.chatId),
            "a second run for a prompt already queued would answer it twice",
        )
        assertEquals(
            1,
            f.module.agentRunRepository.observeRuns(f.chatId).first().size,
            "retry must not queue alongside a live run",
        )
    }

    private class Fixture(val module: TestAgentModule, val chatId: ChatId) {
        private val historyCodec = KoogMessageRowCodec(TestJson, IdGenerator.Random)

        suspend fun commitMessages(messages: List<Message>) {
            module.chatHistoryRepository.commit(
                chatId = chatId,
                messages = historyCodec.toRows(messages),
            )
        }
    }

    private fun retryTest(body: suspend (Fixture) -> Unit): TestResult = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        try {
            val module = TestAgentModule(agentScope = scope)
            body(Fixture(module = module, chatId = module.createChatUseCase(null)))
        } finally {
            scope.cancel()
        }
    }
}
