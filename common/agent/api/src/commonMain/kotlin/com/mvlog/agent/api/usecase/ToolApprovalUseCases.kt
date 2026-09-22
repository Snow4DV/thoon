package com.mvlog.agent.api.usecase

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import kotlinx.coroutines.flow.Flow

/**
 * Records the decision and resumes the turn once every call in it is decided. Idempotent; an
 * unknown key is ignored.
 */
fun interface DecideToolCallUseCase {
    suspend operator fun invoke(chatId: ChatId, toolCallKey: String, decision: ToolApprovalDecision)
}

/** Null chatId: global rules only. Non-null: that chat's own rules only. */
fun interface ObserveToolApprovalRulesUseCase {
    operator fun invoke(chatId: ChatId?): Flow<List<ToolApprovalRule>>
}

/** A blanket rule with no parameters. Null chatId is global. Resumes any turn it settles. */
fun interface AddToolApprovalRuleUseCase {
    suspend operator fun invoke(toolName: String, chatId: ChatId?)
}

fun interface RevokeToolApprovalRuleUseCase {
    suspend operator fun invoke(ruleId: ToolApprovalRuleId)
}

/** Names of the tools whose spec requires approval, for settings rows. */
fun interface GetApprovalGatedToolsUseCase {
    operator fun invoke(): List<String>
}
