package com.mvlog.agent.impl

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import com.mvlog.agent.api.model.ChatExecutionState
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatItem
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalScope
import com.mvlog.agent.api.model.ToolCallState
import com.mvlog.agent.impl.domain.approval.ToolSpecCatalog
import com.mvlog.agent.impl.fake.RecordingTool
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
import kotlin.test.assertIs
import kotlin.time.Clock

/** What the chat screen sees while a turn waits on the user. */
@OptIn(ExperimentalCoroutinesApi::class)
class ToolApprovalStateTest {

    @Test
    fun aPendingGatedCallIsShownAwaitingApprovalWithItsRequestBelow() = stateTest { f ->
        val state = f.module.observeChatUseCase(f.chatId).first()

        val kinds = state.items.map { it::class.simpleName }
        assertEquals(
            listOf("UserMessage", "ToolCall", "ToolApprovalRequest", "ToolCall"),
            kinds,
            "the request follows the call it gates; the free call needs none",
        )
        val gated = assertIs<ChatItem.ToolCall>(state.items[1])
        assertEquals(ToolCallState.AwaitingApproval, gated.state)
        val request = assertIs<ChatItem.ToolApprovalRequest>(state.items[2])
        assertEquals("c1", request.toolCallKey)
        assertEquals(null, request.decision)
        val free = assertIs<ChatItem.ToolCall>(state.items[3])
        assertEquals(ToolCallState.Pending, free.state, "a free call in a stopped turn is requested, not running")
        assertEquals(ChatExecutionState.AwaitingApproval(undecidedCalls = 1), state.execution)
    }

    @Test
    fun aRecordedDecisionShowsOnTheRequest() = stateTest { f ->
        f.module.toolApprovalDecisions.record(f.chatId, "c1", ToolApprovalDecision.Approve(ToolApprovalScope.Once))
        f.module.chatHistoryRepository.commit(
            chatId = f.chatId,
            messages = f.rows(
                MessagePart.Tool.Call(id = "c1", tool = "search", args = "{}"),
                MessagePart.Tool.Call(id = "c2", tool = "search", args = "{}"),
            ),
        )

        val state = f.module.observeChatUseCase(f.chatId).first()

        val requests = state.items.filterIsInstance<ChatItem.ToolApprovalRequest>()
        assertEquals(ToolApprovalDecision.Approve(ToolApprovalScope.Once), requests.single { it.toolCallKey == "c1" }.decision)
        assertEquals(null, requests.single { it.toolCallKey == "c2" }.decision)
        assertEquals(ChatExecutionState.AwaitingApproval(undecidedCalls = 1), state.execution)
    }

    private class Fixture(val module: TestAgentModule, val chatId: ChatId) {
        private val historyCodec = KoogMessageRowCodec(TestJson, IdGenerator.Random)

        fun rows(vararg calls: MessagePart.Tool.Call) = historyCodec.toRows(
            listOf(
                Message.User(parts = listOf(MessagePart.Text("find")), metaInfo = RequestMetaInfo(Clock.System.now())),
                Message.Assistant(parts = calls.toList(), metaInfo = ResponseMetaInfo(Clock.System.now())),
            )
        )
    }

    private fun stateTest(body: suspend (Fixture) -> Unit): TestResult = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        try {
            val search = RecordingTool("search", requiresApproval = true)
            val clock = RecordingTool("clock", requiresApproval = false)
            val module = TestAgentModule(
                agentScope = scope,
                toolSpecCatalog = ToolSpecCatalog { listOf(search.spec, clock.spec) },
            )
            val fixture = Fixture(module = module, chatId = module.createChatUseCase(null))
            module.chatHistoryRepository.commit(
                chatId = fixture.chatId,
                messages = fixture.rows(
                    MessagePart.Tool.Call(id = "c1", tool = "search", args = "{}"),
                    MessagePart.Tool.Call(id = "c2", tool = "clock", args = "{}"),
                ),
            )
            body(fixture)
        } finally {
            scope.cancel()
        }
    }
}
