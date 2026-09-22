package com.mvlog.agent.impl

import ai.koog.agents.snapshot.feature.isTombstone
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import com.mvlog.agent.api.model.ToolApprovalScope
import com.mvlog.agent.impl.execution.AgentExecutionContext
import com.mvlog.agent.impl.fake.RecordingTool
import com.mvlog.agent.impl.fake.StubLLMClient
import com.mvlog.agent.impl.fake.TestAgentModule
import com.mvlog.agent.impl.fake.TestJson
import com.mvlog.agent.impl.fake.registerTools
import com.mvlog.agent.impl.koog.CheckpointCodec
import com.mvlog.agent.impl.koog.KoogAgentRunner
import com.mvlog.agent.impl.koog.KoogMessageRowCodec
import com.mvlog.agent.impl.koog.KoogTarget
import com.mvlog.agent.impl.koog.KoogToolRegistryFactory
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.agent.impl.util.IdGenerator
import com.mvlog.agent.tool.AgentToolsCollector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Clock

/** The graph against real tools: a gated call stops the run, a decided turn resumes it. */
@OptIn(ExperimentalCoroutinesApi::class)
class ToolApprovalFlowTest {

    @Test
    fun aGatedCallEndsTheRunWithTheCallCommittedAndUnanswered() = approvalTest { f ->
        val search = RecordingTool("search", requiresApproval = true)
        registerTools(search)
        val client = StubLLMClient(
            StubLLMClient.replyWithToolCall("Looking", "search", """{"q":"march"}"""),
            StubLLMClient.textReply("Found"),
        )

        f.run(client)

        assertEquals(1, client.prompts.size, "the model must not be asked again before the user decides")
        assertTrue(search.calls.isEmpty(), "a gated tool must not run before approval")

        val last = f.committedMessages().lastOrNull()
        assertTrue(
            last is Message.Assistant && last.parts.any { it is MessagePart.Tool.Call },
            "the turn must be committed ending in the unanswered call, was: $last",
        )
        assertTrue(f.liveCheckpoints().isEmpty(), "the run ended normally, so nothing is restorable")
        assertEquals(1, f.module.conversationRepository.pendingToolCalls(f.chatId).size)
    }

    @Test
    fun aFreeToolStillRunsWithoutAsking() = approvalTest { f ->
        val clock = RecordingTool("clock", requiresApproval = false)
        registerTools(clock)
        val client = StubLLMClient(
            StubLLMClient.replyWithToolCall("Checking", "clock", "{}"),
            StubLLMClient.textReply("It is noon"),
        )

        f.run(client)

        assertEquals(1, clock.calls.size)
        assertEquals(2, client.prompts.size)
    }

    @Test
    fun aRuleLetsAGatedCallRunWithoutStopping() = approvalTest { f ->
        val search = RecordingTool("search", requiresApproval = true)
        registerTools(search)
        f.module.toolApprovalRuleRepository.add(rule(chatId = f.chatId))
        val client = StubLLMClient(
            StubLLMClient.replyWithToolCall("Looking", "search", """{"q":"march"}"""),
            StubLLMClient.textReply("Found"),
        )

        f.run(client)

        assertEquals(1, search.calls.size, "an always-allowed tool runs as if it were not gated")
        assertEquals(2, client.prompts.size)
    }

    @Test
    fun aDecidedTurnResumesRunningTheApprovedCallAndDecliningTheOther() = approvalTest { f ->
        val search = RecordingTool("search", requiresApproval = true)
        val write = RecordingTool("write", requiresApproval = true)
        registerTools(search, write)
        f.commitPendingTurn(
            MessagePart.Tool.Call(id = "c1", tool = "search", args = """{"q":"march"}"""),
            MessagePart.Tool.Call(id = "c2", tool = "write", args = """{"q":"notes"}"""),
        )
        f.module.toolApprovalDecisions.record(f.chatId, "c1", ToolApprovalDecision.Approve(ToolApprovalScope.Once))
        f.module.toolApprovalDecisions.record(f.chatId, "c2", ToolApprovalDecision.Decline)
        val client = StubLLMClient(StubLLMClient.textReply("Done"))

        f.run(client, prompt = "")

        assertEquals(1, search.calls.size, "the approved call runs")
        assertTrue(write.calls.isEmpty(), "the declined call must never run")

        val sent = client.prompts.single().messages.last()
        val results = (sent as? Message.User)?.parts?.filterIsInstance<MessagePart.Tool.Result>()
            ?: fail("the model must receive the results as one user message, was: $sent")
        assertEquals(listOf("c1", "c2"), results.map { it.id }, "every call is answered, in order")
        val declined = results.single { it.id == "c2" }
        assertTrue(declined.isError, "a declined call is reported as an error result")
        assertTrue(
            declined.parts.filterIsInstance<MessagePart.Text>().any { "Declined by the user" in it.text },
            "the model is told why, was: ${declined.parts}",
        )

        val last = f.committedMessages().last()
        assertTrue(last is Message.Assistant && last.textContent() == "Done", "the turn ends with the reply")
    }

    @Test
    fun anUndecidedTurnEndsAgainWithoutCallingTheModel() = approvalTest { f ->
        val search = RecordingTool("search", requiresApproval = true)
        registerTools(search)
        f.commitPendingTurn(MessagePart.Tool.Call(id = "c1", tool = "search", args = "{}"))
        val client = StubLLMClient(StubLLMClient.textReply("never"))

        f.run(client, prompt = "")

        assertTrue(client.prompts.isEmpty(), "nothing to say to the model until the user decides")
        assertTrue(search.calls.isEmpty())
        assertEquals(1, f.module.conversationRepository.pendingToolCalls(f.chatId).size, "still pending")
    }

    private fun rule(chatId: ChatId?) = ToolApprovalRule(
        id = ToolApprovalRuleId("rule"),
        toolName = "search",
        chatId = chatId,
        parameters = emptyMap(),
        createdAt = Clock.System.now(),
    )

    private class Fixture(val module: TestAgentModule, val chatId: ChatId) {
        private val historyCodec = KoogMessageRowCodec(TestJson, IdGenerator.Random)

        suspend fun run(client: StubLLMClient, prompt: String = "find my invoices") {
            KoogAgentRunner(
                target = KoogTarget(executor = StubLLMClient.executor(client), model = StubLLMClient.MODEL),
                chatRepository = module.chatRepository,
                conversationRepository = module.conversationRepository,
                historyProvider = module.chatHistoryProvider,
                persistenceStorage = module.persistenceStorageProvider,
                toolRegistryFactory = KoogToolRegistryFactory(),
                approvals = module.toolApprovalResolver,
                clock = AgentClock.System,
            ).run(AgentExecutionContext(chatId = chatId, runId = AgentRunId("run-${prompt.hashCode()}"), prompt = prompt))
        }

        suspend fun commitPendingTurn(vararg calls: MessagePart.Tool.Call) {
            val now = Clock.System.now()
            module.chatHistoryRepository.commit(
                chatId = chatId,
                messages = historyCodec.toRows(
                    listOf(
                        Message.User(parts = listOf(MessagePart.Text("find my invoices")), metaInfo = RequestMetaInfo(now)),
                        Message.Assistant(parts = calls.toList(), metaInfo = ResponseMetaInfo(now)),
                    )
                ),
            )
        }

        suspend fun committedMessages(): List<Message> =
            historyCodec.toMessages(module.chatHistoryRepository.load(chatId))

        suspend fun liveCheckpoints() = module.checkpointRepository.all(chatId)
            .map { CheckpointCodec(TestJson).decode(it.payload) }
            .filterNot { it.isTombstone() }
    }

    private fun approvalTest(body: suspend (Fixture) -> Unit): TestResult = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        try {
            val module = TestAgentModule(agentScope = scope)
            body(Fixture(module = module, chatId = module.createChatUseCase(null)))
        } finally {
            AgentToolsCollector.reset()
            scope.cancel()
        }
    }
}
