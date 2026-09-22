package com.mvlog.agent.api.di

import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.AddToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.CancelChatRunUseCase
import com.mvlog.agent.api.usecase.DecideToolCallUseCase
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.GetAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetApprovalGatedToolsUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigOverrideUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatsUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveToolApprovalRulesUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.RevokeToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.StartAgentRuntimeUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase

interface ThoonAgentComponent {

    fun startAgentRuntimeUseCase(): StartAgentRuntimeUseCase

    fun observeChatUseCase(): ObserveChatUseCase

    fun observeChatsUseCase(): ObserveChatsUseCase

    fun searchChatsUseCase(): SearchChatsUseCase

    fun createChatUseCase(): CreateChatUseCase

    fun retryChatUseCase(): RetryChatUseCase

    fun deleteChatUseCase(): DeleteChatUseCase

    fun sendPromptUseCase(): SendPromptUseCase

    fun cancelAgentRunUseCase(): CancelAgentRunUseCase

    fun cancelChatRunUseCase(): CancelChatRunUseCase

    fun observeChatConfigOverrideUseCase(): ObserveChatConfigOverrideUseCase

    fun setChatConfigUseCase(): SetChatConfigUseCase

    fun decideToolCallUseCase(): DecideToolCallUseCase

    fun observeToolApprovalRulesUseCase(): ObserveToolApprovalRulesUseCase

    fun addToolApprovalRuleUseCase(): AddToolApprovalRuleUseCase

    fun revokeToolApprovalRuleUseCase(): RevokeToolApprovalRuleUseCase

    fun getApprovalGatedToolsUseCase(): GetApprovalGatedToolsUseCase

    fun observeAgentConfigsUseCase(): ObserveAgentConfigsUseCase

    fun getAgentConfigUseCase(): GetAgentConfigUseCase

    fun observeDefaultAgentConfigUseCase(): ObserveDefaultAgentConfigUseCase

    fun createAgentConfigUseCase(): CreateAgentConfigUseCase

    fun updateAgentConfigUseCase(): UpdateAgentConfigUseCase

    fun deleteAgentConfigUseCase(): DeleteAgentConfigUseCase

    fun setDefaultAgentConfigUseCase(): SetDefaultAgentConfigUseCase
}
