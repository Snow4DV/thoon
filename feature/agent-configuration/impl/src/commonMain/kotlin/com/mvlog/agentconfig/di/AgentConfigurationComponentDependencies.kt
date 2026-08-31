package com.mvlog.agentconfig.di

import com.mvlog.agent.api.di.ThoonAgentComponentHolder
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase

/**
 * What this feature pulls from the agent subsystem.
 *
 * Per-access getters, so nothing resolves before the holder has a provider.
 */
internal interface AgentConfigurationComponentDependencies {

    val observeAgentConfigsUseCase: ObserveAgentConfigsUseCase
    val observeAgentConfigUseCase: ObserveAgentConfigUseCase
    val observeDefaultAgentConfigUseCase: ObserveDefaultAgentConfigUseCase
    val createAgentConfigUseCase: CreateAgentConfigUseCase
    val updateAgentConfigUseCase: UpdateAgentConfigUseCase
    val deleteAgentConfigUseCase: DeleteAgentConfigUseCase
    val setDefaultAgentConfigUseCase: SetDefaultAgentConfigUseCase
    val observeChatConfigUseCase: ObserveChatConfigUseCase
    val setChatConfigUseCase: SetChatConfigUseCase

    class Impl : AgentConfigurationComponentDependencies {
        override val observeAgentConfigsUseCase: ObserveAgentConfigsUseCase
            get() = ThoonAgentComponentHolder.get().observeAgentConfigsUseCase()

        override val observeAgentConfigUseCase: ObserveAgentConfigUseCase
            get() = ThoonAgentComponentHolder.get().observeAgentConfigUseCase()

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

        override val observeChatConfigUseCase: ObserveChatConfigUseCase
            get() = ThoonAgentComponentHolder.get().observeChatConfigUseCase()

        override val setChatConfigUseCase: SetChatConfigUseCase
            get() = ThoonAgentComponentHolder.get().setChatConfigUseCase()
    }
}
