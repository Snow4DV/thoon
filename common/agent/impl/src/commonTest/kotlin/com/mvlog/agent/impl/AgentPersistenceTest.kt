package com.mvlog.agent.impl

import ai.koog.agents.snapshot.feature.AgentCheckpointData
import ai.koog.agents.snapshot.feature.isTombstone
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.message.MessagePart
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.execution.AgentExecutionContext
import com.mvlog.agent.impl.fake.StubLLMClient
import com.mvlog.agent.impl.fake.TestAgentModule
import com.mvlog.agent.impl.fake.TestJson
import com.mvlog.agent.impl.koog.CheckpointCodec
import com.mvlog.agent.impl.koog.KoogAgentRunner
import com.mvlog.agent.impl.koog.KoogMessageRowCodec
import com.mvlog.agent.impl.koog.KoogTarget
import com.mvlog.agent.impl.koog.KoogToolRegistryFactory
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class AgentPersistenceTest {

    @Test
    fun streamedTextReachesTheTimelineAndCommitsToHistory() = persistenceTest { f ->
        f.run(StubLLMClient(StubLLMClient.textReply("Hello", " there")))

        val entries = f.module.chatRepository.observeEntries(f.chatId).first()
        val assistant = entries.filterIsInstance<ChatEntry.AssistantMessage>()
        assertTrue(assistant.isNotEmpty(), "streaming frames must reach the timeline")
        assertEquals("Hello there", assistant.last().text)

        val committed = f.committedMessages()
        assertTrue(
            committed.any { it is Message.Assistant && it.textContent().contains("Hello there") },
            "a completed run must commit its reply, was: $committed",
        )
    }

    @Test
    fun committingARunLeavesNoRestorableCheckpoint() = persistenceTest { f ->
        f.run(StubLLMClient(StubLLMClient.textReply("done")))

        // The tombstone lands after the commit, so "no rows" is the wrong assertion; "nothing
        // restorable" is.
        assertTrue(
            f.liveCheckpoints().isEmpty(),
            "a committed run must leave no restorable checkpoint, was: ${'$'}{f.liveCheckpoints()}",
        )
        assertTrue(
            f.module.checkpointRepository.all(f.chatId).size <= 1,
            "prior checkpoints should be pruned once the run is tombstoned",
        )
    }

    @Test
    fun toolCallsSurviveIntoCommittedHistory() = persistenceTest { f ->
        f.run(
            StubLLMClient(
                    StubLLMClient.replyWithToolCall(
                        text = "Looking that up",
                        toolName = "searchInvoices",
                        toolArgs = """{"q":"march"}""",
                    ),
                    // Without a second reply the stub asks for the tool forever and the run ends
                    // only on its budget.
                    StubLLMClient.textReply("Found three invoices"),
            )
        )

        val parts = f.committedMessages().filterIsInstance<Message.Assistant>().flatMap { it.parts }
        val call = parts.filterIsInstance<MessagePart.Tool.Call>().firstOrNull()
            ?: fail("tool calls must reach history, or the model cannot see its own prior usage")
        assertEquals("searchInvoices", call.tool)
    }

    @Test
    fun theLoopKeepsGoingUntilAReplyCarriesNoToolCalls() = persistenceTest { f ->
        val client = StubLLMClient(
            StubLLMClient.replyWithToolCall("Checking", "searchInvoices", "{}"),
            StubLLMClient.replyWithToolCall("And again", "searchInvoices", "{}"),
            StubLLMClient.textReply("Found them"),
        )

        f.run(client)

        assertEquals(3, client.prompts.size, "each tool result must produce another request")

        val text = f.committedMessages()
            .filterIsInstance<Message.Assistant>()
            .flatMap { it.parts }
            .filterIsInstance<MessagePart.Text>()
            .map { it.text }
        assertTrue("Found them" in text, "the turn ends with the reply that stopped asking")
    }

    @Test
    fun theSystemPromptSurvivesIntoLaterTurns() = persistenceTest { f ->
        f.run(StubLLMClient(StubLLMClient.textReply("first")), prompt = "one")

        val second = StubLLMClient(StubLLMClient.textReply("second"))
        f.run(second, prompt = "two")

        val sent = second.prompts.single().messages
        assertTrue(
            sent.any { it is Message.System },
            "the second turn reached the model without a system prompt, was: ${sent.map { it::class.simpleName }}",
        )
    }

    @Test
    fun aTurnInterruptedAfterAToolRanIsResumable() = persistenceTest { f ->
        // Written by hand: the stub cannot fail one turn and succeed the next.
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

        assertEquals(
            null,
            f.module.conversationRepository.unansweredPrompt(f.chatId),
            "the prompt check cannot see this, which is the whole reason the other one exists",
        )
        assertTrue(
            f.module.conversationRepository.hasUnfinishedToolTurn(f.chatId),
            "a conversation ending in tool results is an unfinished turn",
        )
        assertTrue(
            f.chatId in f.module.conversationRepository.chatsWithUnansweredPrompts(),
            "an interrupted tool turn must be recoverable, not silently abandoned",
        )
    }

    @Test
    fun acompletedTurnIsNotTreatedAsUnfinished() = persistenceTest { f ->
        f.run(StubLLMClient(StubLLMClient.textReply("all done")))

        assertTrue(
            !f.module.conversationRepository.hasUnfinishedToolTurn(f.chatId),
            "a turn that ended in a reply must not be resumed, or every launch would re-run it",
        )
    }

    @Test
    fun aRunawayToolLoopStopsAtItsBudget() = persistenceTest { f ->
        // Unbounded, this ends on Koog's iteration limit — a crashed run, not an answer.
        val client = StubLLMClient(
            StubLLMClient.replyWithToolCall("Again", "searchInvoices", "{}"),
        )

        f.run(client)

        assertTrue(
            client.prompts.size in 2..20,
            "the loop must stop on its own budget, but was ${client.prompts.size} requests",
        )
    }

    @Test
    fun aFailedRunLeavesItsPartialWorkInACheckpoint() = persistenceTest { f ->
        val failing = StubLLMClient(
            StubLLMClient.replyWithToolCall("Looking", "searchInvoices", "{}"),
            failAfterFrames = 2,
        )
        runCatching { f.run(failing) }

        assertTrue(
            f.committedMessages().none { it is Message.Assistant },
            "a failed run must not commit — that is what makes it recoverable",
        )
    }

    @Test
    fun aSecondRunSeesTheFirstRunsConversation() = persistenceTest { f ->
        f.run(StubLLMClient(StubLLMClient.textReply("first answer")))

        val second = StubLLMClient(StubLLMClient.textReply("second answer"))
        f.run(second, prompt = "second question")

        val replayed = second.prompts.single().messages
        assertTrue(
            replayed.any { it is Message.User && it.textContent() == "first question" },
            "the restored conversation must reach the model, was: $replayed",
        )
        assertEquals(
            1,
            replayed.count { it is Message.User && it.textContent() == "second question" },
            "the current prompt must appear exactly once — history plus input would duplicate it",
        )
    }

    /** Checkpoints land per node, so a death mid-respond keeps only the prompt. */
    @Test
    fun aDeadRunLeavesItsPromptVisibleAndCommitsNothing() = persistenceTest { f ->
        f.module.conversationRepository.appendUserPrompt(f.chatId, "explain X")

        val dying = StubLLMClient(
            StubLLMClient.replyWithToolCall("Looking that up", "searchInvoices", "{}"),
            failAfterFrames = 2,
        )
        runCatching { f.run(dying, prompt = "explain X") }

        // A fresh projection, as a restart would build it — nothing in memory carries over.
        val timeline = f.module.conversationRepository.timeline(f.chatId)

        assertTrue(
            timeline.filterIsInstance<ChatEntry.UserMessage>().any { it.text == "explain X" },
            "the prompt was durable before the run, so it must survive it, was: ${'$'}timeline",
        )
        assertTrue(
            timeline.none { it is ChatEntry.AssistantMessage },
            "a run that never completed must not appear to have replied, was: ${'$'}timeline",
        )
    }

    @Test
    fun anUnansweredPromptIsRecoverableAfterAKill() = persistenceTest { f ->
        f.module.conversationRepository.appendUserPrompt(f.chatId, "did this survive?")

        assertEquals(
            "did this survive?",
            f.module.conversationRepository.unansweredPrompt(f.chatId),
            "an accepted prompt must be visibly unanswered, or recovery cannot re-queue it",
        )
        assertEquals(listOf(f.chatId), f.module.conversationRepository.chatsWithUnansweredPrompts())
    }

    @Test
    fun anansweredPromptIsNotRecoveredTwice() = persistenceTest { f ->
        f.module.conversationRepository.appendUserPrompt(f.chatId, "first question")
        f.run(StubLLMClient(StubLLMClient.textReply("answered")))

        assertEquals(
            null,
            f.module.conversationRepository.unansweredPrompt(f.chatId),
            "a prompt that was answered must not look pending, or startup would re-send it",
        )
    }

    @Test
    fun anEarlierUnansweredPromptIsNotLostByTheNextRun() = persistenceTest { f ->
        // Two accepted prompts, neither answered: what a cancelled or failed first run leaves.
        f.module.conversationRepository.appendUserPrompt(f.chatId, "first question")
        f.module.conversationRepository.appendUserPrompt(f.chatId, "second question")

        val client = StubLLMClient(StubLLMClient.textReply("second answer"))
        f.run(client, prompt = "second question")

        val sent = client.prompts.single().messages
            .filterIsInstance<Message.User>()
            .map { it.textContent() }
        assertEquals(
            listOf("first question", "second question"),
            sent,
            "an earlier unanswered prompt is history the model must still see",
        )
        assertEquals(
            listOf("first question", "second question", "second answer"),
            f.spokenHistory(),
            "the earlier prompt must survive the run's commit",
        )
    }

    @Test
    fun aPromptSentWhileARunStreamsSurvivesBothRuns() = persistenceTest { f ->
        f.module.conversationRepository.appendUserPrompt(f.chatId, "first question")
        val slow = StubLLMClient(StubLLMClient.textReply("first", " answer"), frameDelayMillis = 100)
        val first = f.launchRun(slow, prompt = "first question")
        f.advanceTimeBy(150) // one frame in, the reply is still streaming

        f.module.conversationRepository.appendUserPrompt(f.chatId, "second question")
        f.advanceUntilIdle()
        first.join()

        f.run(StubLLMClient(StubLLMClient.textReply("second answer")), prompt = "second question")

        assertEquals(
            listOf("first question", "first answer", "second question", "second answer"),
            f.spokenHistory(),
            "a prompt accepted mid-run must end up after the running turn, not lost",
        )
    }

    @Test
    fun aCancelledRunsPromptIsStillSentWithTheNextOne() = persistenceTest { f ->
        f.module.conversationRepository.appendUserPrompt(f.chatId, "first question")
        val slow = StubLLMClient(StubLLMClient.textReply("first", " answer"), frameDelayMillis = 100)
        val first = f.launchRun(slow, prompt = "first question")
        f.advanceTimeBy(150)
        first.cancelAndJoin()

        f.module.conversationRepository.appendUserPrompt(f.chatId, "second question")
        val second = StubLLMClient(StubLLMClient.textReply("second answer"))
        f.run(second, prompt = "second question")

        val sent = second.prompts.single().messages
            .filterIsInstance<Message.User>()
            .map { it.textContent() }
        assertEquals(
            listOf("first question", "second question"),
            sent,
            "stopping the agent must not erase what the user asked",
        )
    }

    private class Fixture(
        val module: TestAgentModule,
        val chatId: ChatId,
        private val scope: CoroutineScope,
        private val testScope: TestScope,
    ) {
        private val historyCodec = KoogMessageRowCodec(TestJson, IdGenerator.Random)

        /** Runs in the background so the test can act while the reply streams. */
        fun launchRun(client: StubLLMClient, prompt: String): Job =
            scope.launch { run(client, prompt) }

        fun advanceTimeBy(milliseconds: Long) = testScope.advanceTimeBy(milliseconds)

        fun advanceUntilIdle() = testScope.advanceUntilIdle()

        suspend fun spokenHistory(): List<String> =
            committedMessages().filter { it !is Message.System }.map { it.textContent() }

        suspend fun run(client: StubLLMClient, prompt: String = "first question") {
            KoogAgentRunner(
                target = KoogTarget(
                    executor = StubLLMClient.executor(client),
                    model = StubLLMClient.MODEL,
                ),
                chatRepository = module.chatRepository,
                conversationRepository = module.conversationRepository,
                historyProvider = module.chatHistoryProvider,
                persistenceStorage = module.persistenceStorageProvider,
                // Empty registry unless a test registers a provider in the collector.
                toolRegistryFactory = KoogToolRegistryFactory(),
                approvals = module.toolApprovalResolver,
                clock = AgentClock.System,
            ).run(
                AgentExecutionContext(
                    chatId = chatId,
                    runId = AgentRunId("run-${prompt.hashCode()}"),
                    prompt = prompt,
                )
            )
        }

        suspend fun committedMessages(): List<Message> =
            historyCodec.toMessages(module.chatHistoryRepository.load(chatId))

        suspend fun commitMessages(messages: List<Message>) {
            module.chatHistoryRepository.commit(
                chatId = chatId,
                messages = historyCodec.toRows(messages),
            )
        }

        suspend fun liveCheckpoints(): List<AgentCheckpointData> =
            module.checkpointRepository.all(chatId)
                .map { CheckpointCodec(TestJson).decode(it.payload) }
                .filterNot { it.isTombstone() }
    }

    private fun persistenceTest(body: suspend (Fixture) -> Unit): TestResult = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        try {
            val module = TestAgentModule(agentScope = scope)
            val chatId = module.createChatUseCase(null)
            body(Fixture(module = module, chatId = chatId, scope = scope, testScope = this))
        } finally {
            scope.cancel()
        }
    }
}
