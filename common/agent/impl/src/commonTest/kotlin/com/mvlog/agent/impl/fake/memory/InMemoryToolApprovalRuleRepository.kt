package com.mvlog.agent.impl.fake.memory

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import com.mvlog.agent.impl.domain.repository.ToolApprovalRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class InMemoryToolApprovalRuleRepository : ToolApprovalRuleRepository {

    private val rules = MutableStateFlow<List<ToolApprovalRule>>(emptyList())

    override fun observeScoped(chatId: ChatId?): Flow<List<ToolApprovalRule>> =
        rules.map { all -> all.filter { it.chatId == chatId } }

    override fun observeApplicable(chatId: ChatId): Flow<List<ToolApprovalRule>> =
        rules.map { all -> all.filter { it.chatId == null || it.chatId == chatId } }

    override suspend fun applicable(chatId: ChatId): List<ToolApprovalRule> =
        rules.value.filter { it.chatId == null || it.chatId == chatId }

    override suspend fun add(rule: ToolApprovalRule) {
        rules.update { all -> all.filter { it.id != rule.id } + rule }
    }

    override suspend fun remove(id: ToolApprovalRuleId) {
        rules.update { all -> all.filter { it.id != id } }
    }

    override suspend fun deleteForChat(chatId: ChatId) {
        rules.update { all -> all.filter { it.chatId != chatId } }
    }
}
