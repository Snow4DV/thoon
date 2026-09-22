package com.mvlog.agent.impl.di

import com.mvlog.agent.api.di.ThoonAgentComponent
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.AddToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.CancelChatRunUseCase
import com.mvlog.agent.api.usecase.DecideToolCallUseCase
import com.mvlog.agent.api.usecase.GetApprovalGatedToolsUseCase
import com.mvlog.agent.api.usecase.ObserveToolApprovalRulesUseCase
import com.mvlog.agent.api.usecase.RevokeToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigOverrideUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatsUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.StartAgentRuntimeUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase

internal class ThoonAgentComponentImpl(
    dependencies: ThoonAgentComponentDependencies = ThoonAgentComponentDependencies.Impl(),
    private val module: ThoonAgentModule = ThoonAgentModule.Impl(dependencies),
) : ThoonAgentComponent {

    override fun startAgentRuntimeUseCase(): StartAgentRuntimeUseCase =
        module.startAgentRuntimeUseCase

    override fun observeChatUseCase(): ObserveChatUseCase = module.observeChatUseCase

    override fun observeChatsUseCase(): ObserveChatsUseCase = module.observeChatsUseCase

    override fun searchChatsUseCase(): SearchChatsUseCase = module.searchChatsUseCase

    override fun createChatUseCase(): CreateChatUseCase = module.createChatUseCase

    override fun retryChatUseCase(): RetryChatUseCase = module.retryChatUseCase

    override fun deleteChatUseCase(): DeleteChatUseCase = module.deleteChatUseCase

    override fun sendPromptUseCase(): SendPromptUseCase = module.sendPromptUseCase

    override fun cancelAgentRunUseCase(): CancelAgentRunUseCase = module.cancelAgentRunUseCase

    override fun cancelChatRunUseCase(): CancelChatRunUseCase = module.cancelChatRunUseCase

    override fun observeChatConfigOverrideUseCase(): ObserveChatConfigOverrideUseCase =
        module.observeChatConfigOverrideUseCase

    override fun setChatConfigUseCase(): SetChatConfigUseCase = module.setChatConfigUseCase

    override fun decideToolCallUseCase(): DecideToolCallUseCase = module.decideToolCallUseCase

    override fun observeToolApprovalRulesUseCase(): ObserveToolApprovalRulesUseCase =
        module.observeToolApprovalRulesUseCase

    override fun addToolApprovalRuleUseCase(): AddToolApprovalRuleUseCase =
        module.addToolApprovalRuleUseCase

    override fun revokeToolApprovalRuleUseCase(): RevokeToolApprovalRuleUseCase =
        module.revokeToolApprovalRuleUseCase

    override fun getApprovalGatedToolsUseCase(): GetApprovalGatedToolsUseCase =
        module.getApprovalGatedToolsUseCase

    override fun observeAgentConfigsUseCase(): ObserveAgentConfigsUseCase =
        module.observeAgentConfigsUseCase

    override fun getAgentConfigUseCase(): GetAgentConfigUseCase =
        module.getAgentConfigUseCase

    override fun observeDefaultAgentConfigUseCase(): ObserveDefaultAgentConfigUseCase =
        module.observeDefaultAgentConfigUseCase

    override fun createAgentConfigUseCase(): CreateAgentConfigUseCase =
        module.createAgentConfigUseCase

    override fun updateAgentConfigUseCase(): UpdateAgentConfigUseCase =
        module.updateAgentConfigUseCase

    override fun deleteAgentConfigUseCase(): DeleteAgentConfigUseCase =
        module.deleteAgentConfigUseCase

    override fun setDefaultAgentConfigUseCase(): SetDefaultAgentConfigUseCase =
        module.setDefaultAgentConfigUseCase
}
