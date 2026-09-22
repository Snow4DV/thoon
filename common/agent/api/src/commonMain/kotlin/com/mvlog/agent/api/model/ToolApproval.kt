package com.mvlog.agent.api.model

import kotlin.jvm.JvmInline
import kotlin.time.Instant

enum class ToolApprovalScope {
    Once,
    Chat,
    Always,
}

sealed interface ToolApprovalDecision {

    data class Approve(val scope: ToolApprovalScope) : ToolApprovalDecision

    data object Decline : ToolApprovalDecision
}

@JvmInline
value class ToolApprovalRuleId(val value: String)

/** A stored "always allow". Null chatId is global; empty parameters match every call of the tool. */
data class ToolApprovalRule(
    val id: ToolApprovalRuleId,
    val toolName: String,
    val chatId: ChatId?,
    val parameters: Map<String, String>,
    val createdAt: Instant,
)
