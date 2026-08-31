package com.mvlog.chatslist.presentation

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.PersistentList

/**
 * What the chats list shows, and what it reports back.
 *
 * Top-level rather than nested inside `ChatsListScreen`, which lives in the api module: that module
 * exists to be a navigation key, and nesting the state there would make anyone who merely names the
 * screen compile against this feature's row model too.
 */
@Immutable
sealed interface ChatsListUiState : CircuitUiState {

    val eventSink: (ChatsListUiEvent) -> Unit

    data object Loading : ChatsListUiState {
        override val eventSink: (ChatsListUiEvent) -> Unit get() = {}
    }

    data class Data(
        val chats: PersistentList<ChatRow>,
        val tab: ChatsListTab,
        /** What was typed, so the field can be restored; matching happens in storage. */
        val query: String,
        override val eventSink: (ChatsListUiEvent) -> Unit,
    ) : ChatsListUiState {

        /**
         * An empty list means two different things, and conflating them would offer to create a
         * chat as the answer to a search that found none.
         */
        val emptiness: Emptiness
            get() = when {
                chats.isNotEmpty() -> Emptiness.None
                tab == ChatsListTab.Search && query.isNotBlank() -> Emptiness.NoMatches
                else -> Emptiness.NoChats
            }
    }

    enum class Emptiness { None, NoChats, NoMatches }
}

/**
 * The bottom bar's two tabs.
 *
 * Creating a chat is not one: it is an action, and modelling it as a destination would leave the bar
 * showing a selected tab for a screen nobody is on.
 */
enum class ChatsListTab { Chats, Search }

/**
 * One row.
 *
 * [label] is resolved by the presenter rather than the UI because a chat may still have no title —
 * one is taken from the first prompt, so a chat created and abandoned before anything was asked has
 * none. The fallback is a presentation decision, not a rendering detail.
 *
 * [subtitle] is the last thing said, or the matching text when the row came from a search: a result
 * that cannot show why it matched is a filter, not a search.
 *
 * [id] stays the chat's id, because that is what opening and deleting act on. A search can put the
 * same chat in the list several times, so it is [messageSequence] that tells two such rows apart —
 * it names the matching turn, and is null outside a search.
 */
@Immutable
data class ChatRow(
    val id: String,
    val label: String,
    val subtitle: String?,
    val messageSequence: Long? = null,
) {
    /** Unique per row, which [id] no longer is once one chat can match several times. */
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
