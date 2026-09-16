package com.mvlog.chatslist.presentation

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals

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
