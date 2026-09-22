package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import kotlinx.coroutines.flow.Flow

internal interface ToolApprovalRuleRepository {

    /** Exactly that scope: null is the global rules, a chat id that chat's own. */
    fun observeScoped(chatId: ChatId?): Flow<List<ToolApprovalRule>>

    /** Everything that applies to the chat: its own rules plus the global ones. */
    fun observeApplicable(chatId: ChatId): Flow<List<ToolApprovalRule>>

    suspend fun applicable(chatId: ChatId): List<ToolApprovalRule>

    suspend fun add(rule: ToolApprovalRule)

    suspend fun remove(id: ToolApprovalRuleId)

    suspend fun deleteForChat(chatId: ChatId)
}
