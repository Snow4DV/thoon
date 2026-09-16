package com.mvlog.chat.presentation

import com.mvlog.chat.api.ChatScreen
import com.mvlog.chat.presentation.ui.ChatUi
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui

class ChatUiFactory : Ui.Factory {

    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is ChatScreen -> ui<ChatUiState> { state, modifier -> ChatUi(state, modifier) }
        else -> null
    }
}
