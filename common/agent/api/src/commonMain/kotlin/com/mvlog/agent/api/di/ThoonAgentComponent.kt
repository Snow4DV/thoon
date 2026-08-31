package com.mvlog.agent.api.di

import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatsUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.StartAgentRuntimeUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase

/**
 * What the agent subsystem offers its consumers.
 *
 * Use cases rather than repositories: a screen should depend on the operations it performs, not on
 * a service object that also exposes everything it does not. The repositories behind these stay an
 * implementation detail.
 */
interface ThoonAgentComponent {

    // Lifecycle
    fun startAgentRuntimeUseCase(): StartAgentRuntimeUseCase

    // Chat
    fun observeChatUseCase(): ObserveChatUseCase

    fun observeChatsUseCase(): ObserveChatsUseCase

    fun searchChatsUseCase(): SearchChatsUseCase

    fun createChatUseCase(): CreateChatUseCase

    fun retryChatUseCase(): RetryChatUseCase

    fun deleteChatUseCase(): DeleteChatUseCase

    fun sendPromptUseCase(): SendPromptUseCase

    fun cancelAgentRunUseCase(): CancelAgentRunUseCase

    fun observeChatConfigUseCase(): ObserveChatConfigUseCase

    fun setChatConfigUseCase(): SetChatConfigUseCase

    // Configuration
    fun observeAgentConfigsUseCase(): ObserveAgentConfigsUseCase

    fun observeAgentConfigUseCase(): ObserveAgentConfigUseCase

    fun observeDefaultAgentConfigUseCase(): ObserveDefaultAgentConfigUseCase

    fun createAgentConfigUseCase(): CreateAgentConfigUseCase

    fun updateAgentConfigUseCase(): UpdateAgentConfigUseCase

    fun deleteAgentConfigUseCase(): DeleteAgentConfigUseCase

    fun setDefaultAgentConfigUseCase(): SetDefaultAgentConfigUseCase
}
