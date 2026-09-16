package com.mvlog.chatslist.presentation

import com.mvlog.chatslist.api.ChatsListScreen
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen

class ChatsListPresenterFactory(
    private val searchChats: SearchChatsUseCase,
    private val deleteChat: DeleteChatUseCase,
) : Presenter.Factory {

    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? = when (screen) {
        is ChatsListScreen -> ChatsListPresenter(
            navigator = navigator,
            searchChats = searchChats,
            deleteChat = deleteChat,
        )

        else -> null
    }
}
