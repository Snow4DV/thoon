package com.mvlog.chat.di

import com.mvlog.agent.api.di.ThoonAgentComponentHolder
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase

/**
 * What the chat feature needs from other modules, and nothing else.
 *
 * Every member is a lookup against the agent subsystem's holder, resolved per access — so building
 * the chat component does not force the agent to initialise.
 */
internal interface ChatComponentDependencies {

    val observeChatUseCase: ObserveChatUseCase
        get() = ThoonAgentComponentHolder.get().observeChatUseCase()

    val createChatUseCase: CreateChatUseCase
        get() = ThoonAgentComponentHolder.get().createChatUseCase()

    val sendPromptUseCase: SendPromptUseCase
        get() = ThoonAgentComponentHolder.get().sendPromptUseCase()

    val cancelAgentRunUseCase: CancelAgentRunUseCase
        get() = ThoonAgentComponentHolder.get().cancelAgentRunUseCase()

    val retryChatUseCase: RetryChatUseCase
        get() = ThoonAgentComponentHolder.get().retryChatUseCase()

    class Impl : ChatComponentDependencies
}
