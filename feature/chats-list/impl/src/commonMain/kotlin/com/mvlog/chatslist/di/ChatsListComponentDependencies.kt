package com.mvlog.chatslist.di

import com.mvlog.agent.api.di.ThoonAgentComponentHolder
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase

internal interface ChatsListComponentDependencies {

    val searchChatsUseCase: SearchChatsUseCase
        get() = ThoonAgentComponentHolder.get().searchChatsUseCase()

    val deleteChatUseCase: DeleteChatUseCase
        get() = ThoonAgentComponentHolder.get().deleteChatUseCase()

    class Impl : ChatsListComponentDependencies
}
