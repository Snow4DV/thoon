package com.mvlog.chatslist.di

import com.mvlog.agent.api.di.ThoonAgentComponentHolder
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase

/**
 * What the chats-list feature needs from other modules, and nothing else.
 *
 * Every member is a lookup against the agent subsystem's holder, resolved per access — so building
 * this component does not force the agent to initialise.
 */
internal interface ChatsListComponentDependencies {

    val searchChatsUseCase: SearchChatsUseCase
        get() = ThoonAgentComponentHolder.get().searchChatsUseCase()

    val deleteChatUseCase: DeleteChatUseCase
        get() = ThoonAgentComponentHolder.get().deleteChatUseCase()

    class Impl : ChatsListComponentDependencies
}
