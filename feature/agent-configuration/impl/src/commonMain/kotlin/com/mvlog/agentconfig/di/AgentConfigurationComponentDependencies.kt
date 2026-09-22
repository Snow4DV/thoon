package com.mvlog.agentconfig.di

import com.mvlog.agent.api.di.ThoonAgentComponentHolder
import com.mvlog.agent.api.engine.LocalEngineProvider
import com.mvlog.agent.api.engine.LocalEnginesCollector
import com.mvlog.agent.api.usecase.AddToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetApprovalGatedToolsUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigOverrideUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveToolApprovalRulesUseCase
import com.mvlog.agent.api.usecase.RevokeToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase

internal interface AgentConfigurationComponentDependencies {

    val observeAgentConfigsUseCase: ObserveAgentConfigsUseCase
    val getAgentConfigUseCase: GetAgentConfigUseCase
    val observeDefaultAgentConfigUseCase: ObserveDefaultAgentConfigUseCase
    val createAgentConfigUseCase: CreateAgentConfigUseCase
    val updateAgentConfigUseCase: UpdateAgentConfigUseCase
    val deleteAgentConfigUseCase: DeleteAgentConfigUseCase
    val setDefaultAgentConfigUseCase: SetDefaultAgentConfigUseCase
    val observeChatConfigOverrideUseCase: ObserveChatConfigOverrideUseCase
    val setChatConfigUseCase: SetChatConfigUseCase
    val observeToolApprovalRulesUseCase: ObserveToolApprovalRulesUseCase
    val addToolApprovalRuleUseCase: AddToolApprovalRuleUseCase
    val revokeToolApprovalRuleUseCase: RevokeToolApprovalRuleUseCase
    val getApprovalGatedToolsUseCase: GetApprovalGatedToolsUseCase

    val localEngineProvider: LocalEngineProvider

    class Impl : AgentConfigurationComponentDependencies {
        override val observeAgentConfigsUseCase: ObserveAgentConfigsUseCase
            get() = ThoonAgentComponentHolder.get().observeAgentConfigsUseCase()

        override val getAgentConfigUseCase: GetAgentConfigUseCase
            get() = ThoonAgentComponentHolder.get().getAgentConfigUseCase()

        override val observeDefaultAgentConfigUseCase: ObserveDefaultAgentConfigUseCase
            get() = ThoonAgentComponentHolder.get().observeDefaultAgentConfigUseCase()

        override val createAgentConfigUseCase: CreateAgentConfigUseCase
            get() = ThoonAgentComponentHolder.get().createAgentConfigUseCase()

        override val updateAgentConfigUseCase: UpdateAgentConfigUseCase
            get() = ThoonAgentComponentHolder.get().updateAgentConfigUseCase()

        override val deleteAgentConfigUseCase: DeleteAgentConfigUseCase
            get() = ThoonAgentComponentHolder.get().deleteAgentConfigUseCase()

        override val setDefaultAgentConfigUseCase: SetDefaultAgentConfigUseCase
            get() = ThoonAgentComponentHolder.get().setDefaultAgentConfigUseCase()

        override val observeChatConfigOverrideUseCase: ObserveChatConfigOverrideUseCase
            get() = ThoonAgentComponentHolder.get().observeChatConfigOverrideUseCase()

        override val setChatConfigUseCase: SetChatConfigUseCase
            get() = ThoonAgentComponentHolder.get().setChatConfigUseCase()

        override val observeToolApprovalRulesUseCase: ObserveToolApprovalRulesUseCase
            get() = ThoonAgentComponentHolder.get().observeToolApprovalRulesUseCase()

        override val addToolApprovalRuleUseCase: AddToolApprovalRuleUseCase
            get() = ThoonAgentComponentHolder.get().addToolApprovalRuleUseCase()

        override val revokeToolApprovalRuleUseCase: RevokeToolApprovalRuleUseCase
            get() = ThoonAgentComponentHolder.get().revokeToolApprovalRuleUseCase()

        override val getApprovalGatedToolsUseCase: GetApprovalGatedToolsUseCase
            get() = ThoonAgentComponentHolder.get().getApprovalGatedToolsUseCase()

        override val localEngineProvider: LocalEngineProvider
            get() = LocalEngineProvider { LocalEnginesCollector.obtain() }
    }
}
