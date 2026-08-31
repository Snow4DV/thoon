package com.mvlog.chat.presentation

import androidx.compose.runtime.Immutable
import com.mvlog.chat.presentation.item.ChatItem
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.PersistentList

/**
 * What the chat screen shows, and what it reports back.
 *
 * Top-level rather than nested inside `ChatScreen`, which lives in the api module: `State.Data`
 * carries `PersistentList<ChatItem>`, so nesting it there would drag the whole presentation item
 * model into a module whose only job is to be a navigation key — and every feature that merely
 * navigates here would compile against chat's internals. Circuit never needs the link anyway; its
 * factories are star-projected (`Presenter<*>`, `Ui<*>`) and only the `Screen` crosses the boundary.
 */
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
        /** The item a search result pointed at, or null when this chat was not opened from one. */
        val highlightedItemId: String?,
        /**
         * Whether a jump is owed at all.
         *
         * Distinct from [highlightedItemId] being non-null: that resolves only once items arrive,
         * and the list must know to hold off following the newest message before then.
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

        /** Dismisses the options menu without choosing anything. */
        data object ChatOptionsDismissed : Ui

        data object ChatSettingsClicked : Ui

        /** Runs again whatever the chat is still waiting on, and dismisses the error either way. */
        data object ReloadClicked : Ui

        /** The way out of an error caused by configuration rather than by the run itself. */
        data object OpenSettingsClicked : Ui
        data class ThoughtExpandedChanged(val id: String, val isExpanded: Boolean) : Ui
        data class ToolChainCallExpandedChanged(val id: String, val isExpanded: Boolean) : Ui
        data class PromptChanged(val prompt: String) : Ui

        /** Carries its own text so submission never races the debounced [PromptChanged]. */
        data class PromptSubmitted(val prompt: String) : Ui

        data object CancelGenerationClicked : Ui
    }
}
