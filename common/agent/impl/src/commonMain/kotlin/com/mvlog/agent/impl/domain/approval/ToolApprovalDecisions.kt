package com.mvlog.agent.impl.domain.approval

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalDecision
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Process-lifetime, like the run queue it feeds; a lost decision is asked for again. */
internal class ToolApprovalDecisions {

    private val state = MutableStateFlow<Map<ChatId, Map<String, ToolApprovalDecision>>>(emptyMap())

    fun observe(chatId: ChatId): Flow<Map<String, ToolApprovalDecision>> =
        state.map { it[chatId].orEmpty() }.distinctUntilChanged()

    fun snapshot(chatId: ChatId): Map<String, ToolApprovalDecision> = state.value[chatId].orEmpty()

    fun record(chatId: ChatId, toolCallKey: String, decision: ToolApprovalDecision) {
        state.update { all -> all + (chatId to (all[chatId].orEmpty() + (toolCallKey to decision))) }
    }

    fun clear(chatId: ChatId) {
        state.update { all -> all - chatId }
    }
}
