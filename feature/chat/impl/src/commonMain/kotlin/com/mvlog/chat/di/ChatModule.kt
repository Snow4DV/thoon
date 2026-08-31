package com.mvlog.chat.di

import com.mvlog.chat.presentation.ChatPresenterFactory
import com.mvlog.chat.presentation.ChatUiFactory
import com.mvlog.navigation.screen.ScreenFactory

/**
 * Everything the chat feature builds for itself.
 *
 * Swapping this out replaces the whole implementation behind the component — which is how a test or
 * a preview supplies stub use cases without touching the agent subsystem.
 */
internal interface ChatModule {

    val screenFactory: ScreenFactory

    /**
     * Declarations are `get()`: both factories are stateless, so a fresh one per access costs
     * nothing and keeps the agent use cases resolving lazily.
     */
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
                ),
                uiFactory = ChatUiFactory(),
            )
    }
}
