package com.mvlog.chat.presentation

import androidx.compose.runtime.Immutable
import com.mvlog.chat.presentation.item.ChatItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.PersistentList

@Immutable
sealed interface ChatUiState : CircuitUiState {

    val eventSink: (ChatUiEvent) -> Unit
    val chatTitle: String?
    val isChatOptionsMenuVisible: Boolean

    data class Error(
        val description: String,
        override val chatTitle: String?,
        override val eventSink: (ChatUiEvent) -> Unit,
        override val isChatOptionsMenuVisible: Boolean,
    ) : ChatUiState

    data class Loading(
        override val chatTitle: String?,
        override val eventSink: (ChatUiEvent) -> Unit,
        override val isChatOptionsMenuVisible: Boolean,
    ) : ChatUiState

    data class Data(
        val items: PersistentList<ChatItem>,
        val prompt: String,
        val highlightedItemId: String?,
        /**
         * Known before items arrive, unlike [highlightedItemId]; the list must not follow the
         * bottom until the jump is done.
         */
        val isDeepLinked: Boolean,
        val isThinking: Boolean,
        val isRefreshing: Boolean,
        override val isChatOptionsMenuVisible: Boolean,
        override val chatTitle: String,
        override val eventSink: (ChatUiEvent) -> Unit,
    ) : ChatUiState
}

sealed interface ChatUiEvent : CircuitUiEvent {

    sealed interface Ui : ChatUiEvent {
        data object GoBackClicked : Ui
        data object OpenChatOptionsClicked : Ui
        data object ChatOptionsDismissed : Ui
        data object ChatSettingsClicked : Ui
        data object ReloadClicked : Ui
        data object OpenSettingsClicked : Ui
        data class ThoughtExpandedChanged(val id: String, val isExpanded: Boolean) : Ui
        data class ToolChainCallExpandedChanged(val id: String, val isExpanded: Boolean) : Ui
        data class PromptChanged(val prompt: String) : Ui

        /** Carries its own text so submission never races the debounced [PromptChanged]. */
        data class PromptSubmitted(val prompt: String) : Ui

        data object CancelGenerationClicked : Ui
    }
}
