package com.mvlog.chat.di

import com.mvlog.chat.presentation.ChatPresenterFactory
import com.mvlog.chat.presentation.ChatUiFactory
import com.mvlog.navigation.screen.ScreenFactory

internal interface ChatModule {

    val screenFactory: ScreenFactory

    class Impl(
        dependencies: ChatComponentDependencies,
    ) : ChatModule, ChatComponentDependencies by dependencies {

        override val screenFactory: ScreenFactory
            get() = ScreenFactory(
                presenterFactory = ChatPresenterFactory(
                    observeChat = observeChatUseCase,
                    createChat = createChatUseCase,
                    sendPrompt = sendPromptUseCase,
                    cancelAgentRun = cancelAgentRunUseCase,
                    retryChat = retryChatUseCase,
                    decideToolCall = decideToolCallUseCase,
                ),
                uiFactory = ChatUiFactory(),
            )
    }
}
