package com.mvlog.chatslist.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.usecase.CancelChatRunUseCase
import com.mvlog.agent.api.usecase.DeleteChatUseCase
import com.mvlog.agent.api.usecase.SearchChatsUseCase
import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.chat.api.ChatScreen
import com.mvlog.chatslist.presentation.mapper.toRow
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.launch

class ChatsListPresenter(
    private val navigator: Navigator,
    private val searchChats: SearchChatsUseCase,
    private val deleteChat: DeleteChatUseCase,
    private val cancelChatRun: CancelChatRunUseCase,
) : Presenter<ChatsListUiState> {

    @Composable
    override fun present(): ChatsListUiState {
        val scope = rememberCoroutineScope()

        var tab by remember { mutableStateOf(ChatsListTab.Chats) }
        var query by remember { mutableStateOf("") }

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

                // The row settles by itself: isWorking comes from the run repository.
                is ChatsListUiEvent.Ui.StopClicked ->
                    scope.launch { cancelChatRun(ChatId(event.id)) }

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
