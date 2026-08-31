package com.mvlog.chat.presentation

import com.mvlog.chat.api.ChatScreen
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen

/**
 * Resolves [ChatScreen] to its presenter.
 *
 * Returning null for anything else is the contract, not a fallback: the framework asks every
 * registered factory in turn until one claims the screen.
 */
class ChatPresenterFactory(
    private val observeChat: ObserveChatUseCase,
    private val createChat: CreateChatUseCase,
    private val sendPrompt: SendPromptUseCase,
    private val cancelAgentRun: CancelAgentRunUseCase,
    private val retryChat: RetryChatUseCase,
) : Presenter.Factory {

    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? = when (screen) {
        is ChatScreen -> ChatPresenter(
            screen = screen,
            navigator = navigator,
            observeChat = observeChat,
            createChat = createChat,
            sendPrompt = sendPrompt,
            cancelAgentRun = cancelAgentRun,
            retryChat = retryChat,
        )

        else -> null
    }
}
