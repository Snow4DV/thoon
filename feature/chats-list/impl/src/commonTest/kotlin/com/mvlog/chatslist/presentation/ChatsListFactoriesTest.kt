package com.mvlog.chatslist.presentation

import com.mvlog.chatslist.api.ChatsListScreen
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull

class ChatsListFactoriesTest {

    private data object OtherScreen : Screen

    @Test
    fun presenterFactoryClaimsChatsListScreen() {
        val presenter = presenterFactory().create(
            screen = ChatsListScreen,
            navigator = Navigator.NoOp,
            context = CircuitContext.EMPTY,
        )

        assertIs<ChatsListPresenter>(presenter)
    }

    @Test
    fun presenterFactoryDeclinesOtherScreens() {
        assertNull(
            presenterFactory().create(
                screen = OtherScreen,
                navigator = Navigator.NoOp,
                context = CircuitContext.EMPTY,
            ),
            "claiming a screen it does not own would shadow the factory that does",
        )
    }

    @Test
    fun uiFactoryClaimsOnlyChatsListScreen() {
        val factory = ChatsListUiFactory()

        assertNull(
            factory.create(OtherScreen, CircuitContext.EMPTY),
            "claiming a screen it does not own would shadow the factory that does",
        )
        assertIs<Any>(
            factory.create(ChatsListScreen, CircuitContext.EMPTY),
            "the chats list must resolve to a UI",
        )
    }

    private fun presenterFactory() = ChatsListPresenterFactory(
        searchChats = SearchChatsUseCase { flowOf(emptyList()) },
        deleteChat = DeleteChatUseCase { },
    )
}
