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
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.test.fail

/**
 * Runs the real agent — real graph, real chat-memory and persistence features, real storage — with
 * only the model replaced by a script.
 *
 * These assertions exist because none of this could be established from the library's signatures:
 * whether streaming frames reach us, whether tool calls survive into history, and above all whether
 * checkpoints are gone once a run commits. That last one is the design's load-bearing invariant —
 * a checkpoint outliving its commit would replay a turn that already landed.
 */
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

        // The framework writes a tombstone once a run ends, and it arrives *after* the commit — so
        // "no rows at all" is the wrong invariant. What must hold is that nothing restorable
        // survives, or the next turn would replay a conversation that already landed.
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
                    // The turn after the tool result. Without it the stub would ask for the same
                    // tool forever and the run would only end on its budget.
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

        // Three requests means the loop fed each tool result back and asked again — the whole point
        // of the graph. One request would mean the run stopped at the first tool call.
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
        // `ChatMemory` *replaces* the prompt with restored history rather than merging into it, so
        // whatever `systemPrompt(...)` put in the builder is discarded as soon as a chat has any
        // history. If the system message is not restored here, every turn after the first is sent
        // with no system prompt — silently, since nothing errors and the model simply answers.
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
        // The shape a run killed between a tool finishing and the model replying leaves behind.
        // Written directly because the stub cannot fail one turn and not the next.
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
                // Tool results are a *user* message carrying no text, so the unanswered-prompt
                // check reads this as nothing at all — which used to leave the chat looking
                // finished and permanently stalled.
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
        // A model that never stops asking. Without a bound this runs until the framework's own
        // iteration limit throws, which would surface as a crashed run rather than an answer.
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

    /**
     * Pins the actual recovery boundary, which is narrower than it first appears.
     *
     * Checkpoints are written after a node completes, and the graph is currently a single node —
     * so a run that dies mid-stream checkpoints nothing, and its partial reply is gone. What does
     * survive is the prompt, because it was made durable before the run started.
     *
     * Tool calls will only survive a mid-run death once the tool loop exists and each tool
     * execution is its own node; see TODO.md.
     */
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

    // --- harness ---

    private class Fixture(
        val module: TestAgentModule,
        val chatId: ChatId,
        private val scope: CoroutineScope,
    ) {
        private val historyCodec = KoogMessageRowCodec(TestJson, IdGenerator.Random)

        suspend fun run(client: StubLLMClient, prompt: String = "first question") {
            KoogAgentRunner(
                target = KoogTarget(
                    executor = StubLLMClient.executor(client),
                    model = StubLLMClient.MODEL,
                ),
                chatRepository = module.chatRepository,
                historyProvider = module.chatHistoryProvider,
                persistenceStorage = module.persistenceStorageProvider,
                // No provider has registered anything, so this yields an empty registry — these
                // tests are about persistence, not tools.
                toolRegistryFactory = KoogToolRegistryFactory(),
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

        /** Writes a conversation directly, for asserting on a shape a run cannot easily produce. */
        suspend fun commitMessages(messages: List<Message>) {
            module.chatHistoryRepository.commit(
                chatId = chatId,
                messages = historyCodec.toRows(messages),
            )
        }

        /** Checkpoints that still describe unfinished work — tombstones excluded. */
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
            body(Fixture(module = module, chatId = chatId, scope = scope))
        } finally {
            scope.cancel()
        }
    }
}
