package com.mvlog.chatslist.presentation

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * An empty list means two different things.
 *
 * Conflating them would offer "create a new chat" as the answer to a search that found nothing —
 * which answers a question nobody asked and hides the one they did.
 */
class ChatsListEmptinessTest {

    @Test
    fun noChatsAtAllInvitesCreatingOne() {
        assertEquals(ChatsListUiState.Emptiness.NoChats, state().emptiness)
    }

    @Test
    fun aSearchWithNoMatchesDoesNot() {
        assertEquals(
            ChatsListUiState.Emptiness.NoMatches,
            state(tab = ChatsListTab.Search, query = "kyoto").emptiness,
        )
    }

    @Test
    fun anEmptySearchBoxIsNotAFailedSearch() {
        // Having selected the tab but typed nothing, the list is simply empty.
        assertEquals(
            ChatsListUiState.Emptiness.NoChats,
            state(tab = ChatsListTab.Search, query = "  ").emptiness,
        )
    }

    @Test
    fun chatsPresentIsNeitherEmptyState() {
        assertEquals(
            ChatsListUiState.Emptiness.None,
            state(chats = listOf(ChatRow("a", "A", null))).emptiness,
        )
    }

    private fun state(
        chats: List<ChatRow> = emptyList(),
        tab: ChatsListTab = ChatsListTab.Chats,
        query: String = "",
    ) = ChatsListUiState.Data(
        chats = chats.toPersistent(),
        tab = tab,
        query = query,
        eventSink = {},
    )

    private fun List<ChatRow>.toPersistent() =
        if (isEmpty()) persistentListOf() else persistentListOf(*toTypedArray())
}
