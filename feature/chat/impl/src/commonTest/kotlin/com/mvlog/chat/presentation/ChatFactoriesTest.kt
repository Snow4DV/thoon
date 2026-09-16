package com.mvlog.chat.presentation

import com.mvlog.chat.api.ChatScreen
import com.mvlog.agent.api.model.AgentRunId
import com.mvlog.agent.api.model.ChatExecutionState
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The factories are how the framework finds this screen at all, so what matters is that they claim
 * exactly their own screen and decline everything else — a factory that answered for a foreign
 * screen would shadow whichever one actually owns it.
 */
class ChatFactoriesTest {

    private data object OtherScreen : Screen

    @Test
    fun presenterFactoryClaimsChatScreen() {
        val presenter = presenterFactory().create(
            screen = ChatScreen(),
            navigator = Navigator.NoOp,
            context = CircuitContext.EMPTY,
        )

        assertIs<ChatPresenter>(presenter)
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
    fun uiFactoryClaimsOnlyChatScreen() {
        val factory = ChatUiFactory()

        assertEquals(
            null,
            factory.create(OtherScreen, CircuitContext.EMPTY),
            "claiming a screen it does not own would shadow the factory that does",
        )
        assertIs<Any>(
            factory.create(ChatScreen(), CircuitContext.EMPTY),
            "the chat screen must resolve to a UI",
        )
    }

    /**
     * A screen that already names a chat must never mint another one — that was the behaviour that
     * made the screen unusable for anything but a throwaway conversation.
     */
    @Test
    fun anExistingChatIsNotRecreated() {
        var created = 0
        val presenter = ChatPresenterFactory(
            observeChat = ObserveChatUseCase { flowOf(emptyChatState(it)) },
            createChat = CreateChatUseCase { created++; ChatId("fresh") },
            sendPrompt = SendPromptUseCase { _, _ -> AgentRunId("run") },
            cancelAgentRun = CancelAgentRunUseCase { },
            retryChat = RetryChatUseCase { false },
        ).create(
            screen = ChatScreen(chatId = ChatId("existing")),
            navigator = Navigator.NoOp,
            context = CircuitContext.EMPTY,
        )

        assertIs<ChatPresenter>(presenter)
        assertEquals(0, created, "constructing a presenter must not create a chat on its own")
    }

    private fun presenterFactory() = ChatPresenterFactory(
        observeChat = ObserveChatUseCase { flowOf(emptyChatState(it)) },
        createChat = CreateChatUseCase { ChatId("chat") },
        sendPrompt = SendPromptUseCase { _, _ -> AgentRunId("run") },
        cancelAgentRun = CancelAgentRunUseCase { },
        retryChat = RetryChatUseCase { false },
    )

    private fun emptyChatState(chatId: ChatId) = ChatState(
        chatId = chatId,
        items = emptyList(),
        execution = ChatExecutionState.Idle,
    )
}
