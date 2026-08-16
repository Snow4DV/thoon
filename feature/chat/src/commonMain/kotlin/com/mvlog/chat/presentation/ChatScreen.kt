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
        val chatTitle: String?
        val isChatOptionsMenuVisible: Boolean

        data class Error(
            val description: String,
            override val chatTitle: String?,
            override val eventSink: (Event) -> Unit,
            override val isChatOptionsMenuVisible: Boolean,
        ) : State

        data class Loading(
            override val chatTitle: String?,
            override val eventSink: (Event) -> Unit,
            override val isChatOptionsMenuVisible: Boolean,
        ) : State

        data class Data(
            val items: PersistentList<ChatItem>,
            val prompt: String,
            val isThinking: Boolean,
            val isRefreshing: Boolean,
            override val isChatOptionsMenuVisible: Boolean,
            override val chatTitle: String,
            override val eventSink: (Event) -> Unit,
        ) : State
    }

    sealed interface Event : CircuitUiEvent {

        sealed interface Ui : Event {
            data object GoBackClicked : Ui
            data object OpenChatOptionsClicked : Ui
            data object ReloadClicked : Ui
            data class ThoughtExpandedChanged(val id: String, val isExpanded: Boolean) : Ui
            data class ToolChainCallExpandedChanged(val id: String, val isExpanded: Boolean) : Ui
            data class PromptChanged(val prompt: String) : Ui

            /** Carries its own text so submission never races the debounced [PromptChanged]. */
            data class PromptSubmitted(val prompt: String) : Ui

            data object CancelGenerationClicked : Ui
        }
    }
}
