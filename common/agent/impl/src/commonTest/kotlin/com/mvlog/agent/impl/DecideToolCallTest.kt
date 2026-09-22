package com.mvlog.agent.impl

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalScope
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
import kotlin.test.assertTrue
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class DecideToolCallTest {

    @Test
    fun allowingOnceQueuesTheResumeAndStoresNoRule() = decideTest { f ->
        f.module.decideToolCallUseCase(f.chatId, "c1", ToolApprovalDecision.Approve(ToolApprovalScope.Once))

        assertEquals("", f.module.agentRunRepository.nextQueuedRun(f.chatId)?.prompt, "a blank run resumes the turn")
        assertTrue(f.module.toolApprovalRuleRepository.applicable(f.chatId).isEmpty())
    }

    @Test
    fun allowingForTheChatStoresAChatRuleBoundToTheValue() = decideTest { f ->
        f.module.decideToolCallUseCase(f.chatId, "c1", ToolApprovalDecision.Approve(ToolApprovalScope.Chat))

        val rule = f.module.toolApprovalRuleRepository.applicable(f.chatId).single()
        assertEquals(f.chatId, rule.chatId)
        assertEquals(mapOf("q" to "march"), rule.parameters, "the per-value parameter is remembered")
    }

    @Test
    fun allowingEverywhereStoresAGlobalRule() = decideTest { f ->
        f.module.decideToolCallUseCase(f.chatId, "c1", ToolApprovalDecision.Approve(ToolApprovalScope.Always))

        val rule = f.module.toolApprovalRuleRepository.observeScoped(null).first().single()
        assertEquals(null, rule.chatId)
        assertEquals("search", rule.toolName)
    }

    @Test
    fun anUnknownKeyChangesNothing() = decideTest { f ->
        f.module.decideToolCallUseCase(f.chatId, "no-such-call", ToolApprovalDecision.Decline)

        assertEquals(null, f.module.agentRunRepository.nextQueuedRun(f.chatId))
        assertTrue(f.module.toolApprovalDecisions.snapshot(f.chatId).isEmpty())
    }

    @Test
    fun decidingTwiceQueuesOneRun() = decideTest { f ->
        f.module.decideToolCallUseCase(f.chatId, "c1", ToolApprovalDecision.Decline)
        f.module.decideToolCallUseCase(f.chatId, "c1", ToolApprovalDecision.Decline)

        assertEquals(1, f.module.agentRunRepository.observeRuns(f.chatId).first().size, "a double tap is harmless")
    }

    private class Fixture(val module: TestAgentModule, val chatId: ChatId)

    private fun decideTest(body: suspend (Fixture) -> Unit): TestResult = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        try {
            val search = RecordingTool("search", requiresApproval = true, approveQueryPerValue = true)
            val module = TestAgentModule(agentScope = scope, toolSpecCatalog = ToolSpecCatalog { listOf(search.spec) })
            val chatId = module.createChatUseCase(null)
            val now = Clock.System.now()
            module.chatHistoryRepository.commit(
                chatId = chatId,
                messages = KoogMessageRowCodec(TestJson, IdGenerator.Random).toRows(
                    listOf(
                        Message.User(parts = listOf(MessagePart.Text("find")), metaInfo = RequestMetaInfo(now)),
                        Message.Assistant(
                            parts = listOf(MessagePart.Tool.Call(id = "c1", tool = "search", args = """{"q":"march"}""")),
                            metaInfo = ResponseMetaInfo(now),
                        ),
                    )
                ),
            )
            body(Fixture(module = module, chatId = chatId))
        } finally {
            scope.cancel()
        }
    }
}
