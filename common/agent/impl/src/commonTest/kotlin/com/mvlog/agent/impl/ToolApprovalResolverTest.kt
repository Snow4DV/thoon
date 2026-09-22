package com.mvlog.agent.impl

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import com.mvlog.agent.api.model.ToolApprovalScope
import com.mvlog.agent.impl.domain.approval.PendingToolCall
import com.mvlog.agent.impl.domain.approval.ToolApprovalDecisions
import com.mvlog.agent.impl.domain.approval.ToolApprovalResolver
import com.mvlog.agent.impl.domain.approval.ToolCallVerdict
import com.mvlog.agent.impl.domain.approval.ToolSpecCatalog
import com.mvlog.agent.impl.fake.RecordingTool
import com.mvlog.agent.impl.fake.memory.InMemoryToolApprovalRuleRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class ToolApprovalResolverTest {

    private val gated = RecordingTool("search", requiresApproval = true, approveQueryPerValue = true)
    private val free = RecordingTool("clock", requiresApproval = false)

    private val resolver = ToolApprovalResolver(
        catalog = ToolSpecCatalog { listOf(gated.spec, free.spec) },
        rules = InMemoryToolApprovalRuleRepository(),
        decisions = ToolApprovalDecisions(),
    )

    @Test
    fun aToolThatDoesNotAskForApprovalIsAllowed() {
        assertEquals(ToolCallVerdict.Allowed, verdict(call("clock")))
    }

    @Test
    fun aGatedToolWithNoRuleAndNoDecisionIsUndecided() {
        assertEquals(ToolCallVerdict.Undecided, verdict(call("search")))
    }

    @Test
    fun aBlanketRuleAllowsEveryValue() {
        assertEquals(ToolCallVerdict.Allowed, verdict(call("search", q = "a"), rules = listOf(rule(GLOBAL))))
        assertEquals(ToolCallVerdict.Allowed, verdict(call("search", q = "b"), rules = listOf(rule(CHAT))))
    }

    @Test
    fun aValueBoundRuleCoversOnlyTheSameValue() {
        val rules = listOf(rule(CHAT, parameters = mapOf("q" to "march")))

        assertEquals(ToolCallVerdict.Allowed, verdict(call("search", q = "march"), rules = rules))
        assertEquals(
            ToolCallVerdict.Undecided,
            verdict(call("search", q = "april"), rules = rules),
            "a rule remembered for one value must not cover another",
        )
    }

    @Test
    fun aDeclineOutranksARule() {
        assertEquals(
            ToolCallVerdict.Declined,
            verdict(
                call("search"),
                decisions = mapOf("c1" to ToolApprovalDecision.Decline),
                rules = listOf(rule(GLOBAL)),
            ),
            "saying no to one call must hold even when the tool is always allowed",
        )
    }

    @Test
    fun anApprovalIsReportedAsSuch() {
        assertEquals(
            ToolCallVerdict.Approved,
            verdict(call("search"), decisions = mapOf("c1" to ToolApprovalDecision.Approve(ToolApprovalScope.Once))),
        )
    }

    @Test
    fun ruleParametersKeepOnlyTheValuesApprovedPerValue() {
        assertEquals(
            mapOf("q" to "march"),
            resolver.ruleParameters(call("search", q = "march", extra = "ignored")),
        )
        assertEquals(emptyMap(), resolver.ruleParameters(call("clock", q = "x")))
    }

    private fun verdict(
        call: PendingToolCall,
        decisions: Map<String, ToolApprovalDecision> = emptyMap(),
        rules: List<ToolApprovalRule> = emptyList(),
    ): ToolCallVerdict = resolver.verdicts(listOf(call), decisions, rules).getValue(call.key)

    private fun call(name: String, q: String? = null, extra: String? = null) = PendingToolCall(
        key = "c1",
        id = "c1",
        name = name,
        argumentsJson = buildString {
            append("{")
            append(listOfNotNull(q?.let { "\"q\":\"$it\"" }, extra?.let { "\"extra\":\"$it\"" }).joinToString(","))
            append("}")
        },
    )

    private fun rule(chatId: ChatId?, parameters: Map<String, String> = emptyMap()) = ToolApprovalRule(
        id = ToolApprovalRuleId("r-${parameters.hashCode()}"),
        toolName = "search",
        chatId = chatId,
        parameters = parameters,
        createdAt = Clock.System.now(),
    )

    private companion object {
        val GLOBAL: ChatId? = null
        val CHAT = ChatId("chat")
    }
}
