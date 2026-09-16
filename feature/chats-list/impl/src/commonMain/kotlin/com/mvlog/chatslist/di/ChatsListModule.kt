package com.mvlog.chatslist.di

import com.mvlog.chatslist.presentation.ChatsListPresenterFactory
import com.mvlog.chatslist.presentation.ChatsListUiFactory
import com.mvlog.navigation.screen.ScreenFactory

internal interface ChatsListModule {

    val screenFactory: ScreenFactory

    class Impl(
        dependencies: ChatsListComponentDependencies,
    ) : ChatsListModule, ChatsListComponentDependencies by dependencies {

        override val screenFactory: ScreenFactory
            get() = ScreenFactory(
                presenterFactory = ChatsListPresenterFactory(
                    searchChats = searchChatsUseCase,
                    deleteChat = deleteChatUseCase,
                ),
                uiFactory = ChatsListUiFactory(),
            )
    }
}
