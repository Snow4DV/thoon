package com.mvlog.chat.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mvlog.agentconfig.api.ChatConfigurationScreen
import com.mvlog.chat.api.ChatScreen
import com.mvlog.agent.api.model.ChatExecutionState
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatItem
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.chat.presentation.mapper.toUi
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatPresenter(
    private val screen: ChatScreen,
    private val navigator: Navigator,
    private val observeChat: ObserveChatUseCase,
    private val createChat: CreateChatUseCase,
    private val sendPrompt: SendPromptUseCase,
    private val cancelAgentRun: CancelAgentRunUseCase,
    private val retryChat: RetryChatUseCase,
    private val chatTitle: String = DEFAULT_TITLE,
) : Presenter<ChatUiState> {

    @Composable
    override fun present(): ChatUiState {
        val scope = rememberCoroutineScope()

        var prompt by remember { mutableStateOf("") }
        var expandedIds by remember { mutableStateOf(emptySet<String>()) }
        var isChatOptionsMenuVisible by remember { mutableStateOf(false) }
        var failure by remember { mutableStateOf<String?>(null) }

        var createdChatId by rememberRetained { mutableStateOf<ChatId?>(null) }

        val chatId = screen.chatId ?: createdChatId

        LaunchedEffect(screen.chatId) {
            if (screen.chatId == null && createdChatId == null) {
                createdChatId = createChat(configId = null)
            }
        }

        val chatState by produceState<ChatState?>(initialValue = null, chatId) {
            val id = chatId ?: return@produceState
            observeChat(id).collectLatest { value = it }
        }

        // A failed run shows once as the Error state; the timeline underneath is kept.
        LaunchedEffect(chatState?.execution) {
            failure = (chatState?.execution as? ChatExecutionState.Failed)?.message
        }

        val eventSink: (ChatUiEvent) -> Unit = { event ->
            when (event) {
                is ChatUiEvent.Ui.PromptChanged -> prompt = event.prompt

                is ChatUiEvent.Ui.PromptSubmitted -> {
                    val id = chatId
                    if (id != null && event.prompt.isNotBlank()) {
                        prompt = ""
                        scope.launch { sendPrompt(chatId = id, text = event.prompt) }
                    }
                }

                is ChatUiEvent.Ui.CancelGenerationClicked -> {
                    val working = chatState?.execution as? ChatExecutionState.Working
                    if (working != null) {
                        scope.launch { cancelAgentRun(working.runId) }
                    }
                }

                is ChatUiEvent.Ui.ThoughtExpandedChanged ->
                    expandedIds = expandedIds.toggle(event.id, event.isExpanded)

                is ChatUiEvent.Ui.ToolChainCallExpandedChanged ->
                    expandedIds = expandedIds.toggle(event.id, event.isExpanded)

                ChatUiEvent.Ui.OpenChatOptionsClicked ->
                    isChatOptionsMenuVisible = !isChatOptionsMenuVisible

                ChatUiEvent.Ui.ChatOptionsDismissed -> isChatOptionsMenuVisible = false

                ChatUiEvent.Ui.ChatSettingsClicked -> {
                    isChatOptionsMenuVisible = false
                    chatId?.let { navigator.goTo(ChatConfigurationScreen(it)) }
                }

                ChatUiEvent.Ui.ReloadClicked -> {
                    // Cleared even if retry finds nothing to run, or a stale error has no way off
                    // the screen.
                    failure = null
                    chatId?.let { scope.launch { retryChat(it) } }
                }

                ChatUiEvent.Ui.OpenSettingsClicked ->
                    chatId?.let { navigator.goTo(ChatConfigurationScreen(it)) }

                ChatUiEvent.Ui.GoBackClicked -> navigator.pop()
            }
        }

        val currentFailure = failure
        val currentState = chatState

        return when {
            currentFailure != null -> ChatUiState.Error(
                description = currentFailure,
                chatTitle = chatTitle,
                eventSink = eventSink,
                isChatOptionsMenuVisible = isChatOptionsMenuVisible,
            )

            currentState == null -> ChatUiState.Loading(
                chatTitle = chatTitle,
                eventSink = eventSink,
                isChatOptionsMenuVisible = isChatOptionsMenuVisible,
            )

            else -> ChatUiState.Data(
                items = currentState.items.map { it.toUi(expandedIds) }.toPersistentList(),
                highlightedItemId = resolveHighlightedItemId(
                    items = currentState.items,
                    messageSequence = screen.highlightMessageSequence,
                ),
                isDeepLinked = screen.highlightMessageSequence != null,
                prompt = prompt,
                isThinking = currentState.isWorking,
                isRefreshing = false,
                isChatOptionsMenuVisible = isChatOptionsMenuVisible,
                chatTitle = chatTitle,
                eventSink = eventSink,
            )
        }
    }

    private fun Set<String>.toggle(id: String, isSelected: Boolean): Set<String> =
        if (isSelected) this + id else this - id

    private companion object {
        const val DEFAULT_TITLE = "Thoon"
    }
}

internal fun resolveHighlightedItemId(items: List<ChatItem>, messageSequence: Long?): String? {
    if (messageSequence == null) return null

    val candidates = items.filter { it.messageSequence == messageSequence }
    val message = candidates.firstOrNull {
        it is ChatItem.UserMessage || it is ChatItem.AssistantMessage
    }
    return (message ?: candidates.firstOrNull())?.id
}
