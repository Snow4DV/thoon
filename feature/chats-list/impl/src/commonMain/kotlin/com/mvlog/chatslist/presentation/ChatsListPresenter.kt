package com.mvlog.chatslist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.chat.api.ChatScreen
import com.mvlog.chatslist.presentation.mapper.toRow
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

/**
 * Lists chats, searches them, and opens them.
 *
 * One source for both tabs: searching with a blank query lists everything, so the screen never
 * switches between two flows and the list cannot disagree with itself mid-transition.
 *
 * Creating a chat is deliberately not done here: navigating to [ChatScreen] with a null id makes
 * that screen create one. Doing it in both places would leave an empty chat behind whenever the
 * user backed out immediately.
 */
class ChatsListPresenter(
    private val navigator: Navigator,
    private val searchChats: SearchChatsUseCase,
    private val deleteChat: DeleteChatUseCase,
) : Presenter<ChatsListUiState> {

    @Composable
    override fun present(): ChatsListUiState {
        val scope = rememberCoroutineScope()

        var tab by remember { mutableStateOf(ChatsListTab.Chats) }
        var query by remember { mutableStateOf("") }

        // Leaving the search tab clears the query rather than remembering it: coming back to a list
        // silently filtered by something typed minutes ago reads as chats having gone missing.
        val effectiveQuery = if (tab == ChatsListTab.Search) query else ""
        val results by remember(effectiveQuery) { searchChats(effectiveQuery) }
            .collectAsState(initial = null)

        val eventSink: (ChatsListUiEvent) -> Unit = { event ->
            when (event) {
                is ChatsListUiEvent.Ui.ChatClicked ->
                    navigator.goTo(
                        ChatScreen(
                            chatId = ChatId(event.id),
                            highlightMessageSequence = event.messageSequence,
                        )
                    )

                ChatsListUiEvent.Ui.NewChatClicked ->
                    navigator.goTo(ChatScreen(chatId = null))

                is ChatsListUiEvent.Ui.DeleteClicked ->
                    scope.launch { deleteChat(ChatId(event.id)) }

                ChatsListUiEvent.Ui.SettingsClicked ->
                    navigator.goTo(AgentConfigurationScreen())

                is ChatsListUiEvent.Ui.TabSelected -> {
                    tab = event.tab
                    if (event.tab == ChatsListTab.Chats) query = ""
                }

                is ChatsListUiEvent.Ui.QueryChanged -> query = event.query
            }
        }

        val current = results ?: return ChatsListUiState.Loading

        return ChatsListUiState.Data(
            chats = current.map { it.toRow() }.toPersistentList(),
            tab = tab,
            query = query,
            eventSink = eventSink,
        )
    }
}
