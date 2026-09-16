package com.mvlog.chatslist.presentation

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.PersistentList

@Immutable
sealed interface ChatsListUiState : CircuitUiState {

    val eventSink: (ChatsListUiEvent) -> Unit

    data object Loading : ChatsListUiState {
        override val eventSink: (ChatsListUiEvent) -> Unit get() = {}
    }

    data class Data(
        val chats: PersistentList<ChatRow>,
        val tab: ChatsListTab,
        /** Kept to restore the field; matching happens in storage. */
        val query: String,
        override val eventSink: (ChatsListUiEvent) -> Unit,
    ) : ChatsListUiState {

        val emptiness: Emptiness
            get() = when {
                chats.isNotEmpty() -> Emptiness.None
                tab == ChatsListTab.Search && query.isNotBlank() -> Emptiness.NoMatches
                else -> Emptiness.NoChats
            }
    }

    enum class Emptiness { None, NoChats, NoMatches }
}

enum class ChatsListTab { Chats, Search }

@Immutable
data class ChatRow(
    val id: String,
    val label: String,
    val subtitle: String?,
    val messageSequence: Long? = null,
) {
    /**
     * One chat can match several times; [messageSequence] tells those rows apart, [id] is what
     * opening and deleting act on.
     */
    val key: String get() = "$id#$messageSequence"
}

sealed interface ChatsListUiEvent : CircuitUiEvent {

    sealed interface Ui : ChatsListUiEvent {

        data class ChatClicked(val id: String, val messageSequence: Long? = null) : Ui

        data class DeleteClicked(val id: String) : Ui

        data object NewChatClicked : Ui

        data object SettingsClicked : Ui

        data class TabSelected(val tab: ChatsListTab) : Ui

        data class QueryChanged(val query: String) : Ui
    }
}
