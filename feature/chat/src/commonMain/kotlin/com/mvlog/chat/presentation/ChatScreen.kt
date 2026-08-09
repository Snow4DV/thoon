package com.mvlog.chat.presentation

import androidx.compose.runtime.Immutable
import com.mvlog.chat.presentation.item.ChatItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import com.mvlog.navigation.CommonParcelize
import kotlinx.collections.immutable.PersistentList

@CommonParcelize
data object ChatScreen : Screen {

    @Immutable
    sealed interface State : CircuitUiState {

        val eventSink: (Event) -> Unit

        data class Error(
            val description: String,
            override val eventSink: (Event) -> Unit,
        ) : State

        data class Loading(override val eventSink: (Event) -> Unit) : State

        data class Data(
            val chatTitle: String,
            val items: PersistentList<ChatItem>,
            val prompt: String,
            val isThinking: Boolean,
            val isRefreshing: Boolean,
            val isChatOptionsMenuVisible: Boolean,
            override val eventSink: (Event) -> Unit,
        ) : State
    }

    sealed interface Event : CircuitUiEvent {

        sealed interface Ui : Event {
            data object GoBack : Ui
            data object OpenChatOptions : Ui
        }
    }
}
