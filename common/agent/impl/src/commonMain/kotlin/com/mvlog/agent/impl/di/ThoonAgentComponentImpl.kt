package com.mvlog.agent.impl.di

import com.mvlog.agent.api.di.ThoonAgentComponent
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatsUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.StartAgentRuntimeUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase

/**
 * Exposes the module's use cases to consumers.
 *
 * Builds nothing itself — that belongs to [ThoonAgentModule]. Supplying a different module replaces
 * the whole implementation behind this surface.
 */
internal class ThoonAgentComponentImpl(
    dependencies: ThoonAgentComponentDependencies = ThoonAgentComponentDependencies.Impl(),
    private val module: ThoonAgentModule = ThoonAgentModule.Impl(dependencies),
) : ThoonAgentComponent {

    // Lifecycle
    override fun startAgentRuntimeUseCase(): StartAgentRuntimeUseCase =
        module.startAgentRuntimeUseCase

    // Chat
    override fun observeChatUseCase(): ObserveChatUseCase = module.observeChatUseCase

    override fun observeChatsUseCase(): ObserveChatsUseCase = module.observeChatsUseCase

    override fun searchChatsUseCase(): SearchChatsUseCase = module.searchChatsUseCase

    override fun createChatUseCase(): CreateChatUseCase = module.createChatUseCase

    override fun retryChatUseCase(): RetryChatUseCase = module.retryChatUseCase

    override fun deleteChatUseCase(): DeleteChatUseCase = module.deleteChatUseCase

    override fun sendPromptUseCase(): SendPromptUseCase = module.sendPromptUseCase

    override fun cancelAgentRunUseCase(): CancelAgentRunUseCase = module.cancelAgentRunUseCase

    override fun observeChatConfigUseCase(): ObserveChatConfigUseCase =
        module.observeChatConfigUseCase

    override fun setChatConfigUseCase(): SetChatConfigUseCase = module.setChatConfigUseCase

    // Configuration
    override fun observeAgentConfigsUseCase(): ObserveAgentConfigsUseCase =
        module.observeAgentConfigsUseCase

    override fun observeAgentConfigUseCase(): ObserveAgentConfigUseCase =
        module.observeAgentConfigUseCase

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
