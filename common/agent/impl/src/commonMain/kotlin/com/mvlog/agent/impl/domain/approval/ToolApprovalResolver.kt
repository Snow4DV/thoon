package com.mvlog.agent.impl.domain.approval

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.impl.domain.repository.ToolApprovalRuleRepository
import com.mvlog.agent.tool.AgentToolSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** One reading of "may this call run", shared by the graph, recovery and the timeline. */
internal class ToolApprovalResolver(
    private val catalog: ToolSpecCatalog,
    private val rules: ToolApprovalRuleRepository,
    private val decisions: ToolApprovalDecisions,
) {

    fun isGatedTurn(calls: List<PendingToolCall>): Boolean {
        val specs = catalog.specs().associateBy { it.name }
        return calls.any { specs[it.name]?.requiresApproval == true }
    }

    suspend fun verdicts(chatId: ChatId, calls: List<PendingToolCall>): Map<String, ToolCallVerdict> =
        verdicts(calls, decisions.snapshot(chatId), rules.applicable(chatId))

    fun verdicts(
        calls: List<PendingToolCall>,
        decisions: Map<String, ToolApprovalDecision>,
        rules: List<ToolApprovalRule>,
    ): Map<String, ToolCallVerdict> {
        val specs = catalog.specs().associateBy { it.name }
        return calls.associate { call ->
            call.key to verdict(call, specs[call.name], decisions[call.key], rules)
        }
    }

    /** What an "always" decision remembers about this call: its per-value parameters. */
    fun ruleParameters(call: PendingToolCall): Map<String, String> {
        val spec = catalog.specs().firstOrNull { it.name == call.name } ?: return emptyMap()
        val arguments = call.arguments()
        return spec.parameters
            .filter { it.requiresApprovalPerValue }
            .mapNotNull { parameter -> arguments[parameter.name]?.let { parameter.name to it } }
            .toMap()
    }

    private fun verdict(
        call: PendingToolCall,
        spec: AgentToolSpec?,
        decision: ToolApprovalDecision?,
        rules: List<ToolApprovalRule>,
    ): ToolCallVerdict {
        if (spec?.requiresApproval != true) return ToolCallVerdict.Allowed

        // A decision outranks a rule: the user may say no to one call of an always-allowed tool.
        when (decision) {
            is ToolApprovalDecision.Approve -> return ToolCallVerdict.Approved
            ToolApprovalDecision.Decline -> return ToolCallVerdict.Declined
            null -> Unit
        }

        val arguments = call.arguments()
        val matched = rules.any { rule ->
            rule.toolName == call.name && rule.parameters.all { (name, value) -> arguments[name] == value }
        }
        return if (matched) ToolCallVerdict.Allowed else ToolCallVerdict.Undecided
    }

    private fun PendingToolCall.arguments(): Map<String, String> =
        runCatching { Json.parseToJsonElement(argumentsJson).jsonObject }
            .getOrNull()
            .orEmpty()
            .mapValues { (_, element) -> (element as? JsonPrimitive)?.content ?: element.toString() }
}
