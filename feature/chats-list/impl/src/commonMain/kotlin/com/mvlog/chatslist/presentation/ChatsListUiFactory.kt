package com.mvlog.chatslist.presentation

import com.mvlog.chatslist.api.ChatsListScreen
import com.mvlog.chatslist.presentation.ui.ChatsListUi
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui

class ChatsListUiFactory : Ui.Factory {

    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is ChatsListScreen -> ui<ChatsListUiState> { state, modifier ->
            ChatsListUi(state, modifier)
        }

        else -> null
    }
}
