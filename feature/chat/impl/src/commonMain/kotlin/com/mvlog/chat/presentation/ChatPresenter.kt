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

/**
 * Holds only what is genuinely presentational — the draft prompt and which items are expanded.
 *
 * Conversation content and progress come from storage rather than from this presenter's lifetime.
 * Leaving the screen mid-answer therefore loses nothing: on return, the observed state already
 * reflects whatever the agent produced in the meantime.
 */
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

        /**
         * Retained, not remembered: a configuration change or a pop-and-return would otherwise
         * re-run this and silently start a second conversation, abandoning the first.
         */
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

        // Surface a failed run once, as a screen-level error, then let the timeline stand.
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
                        // Fire-and-forget: `prompt` returns once the run is durable, and the answer
                        // arrives through the observed state rather than through this call.
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
                    // Only a saved chat has configuration to change; a new one has no id until its
                    // first prompt creates it.
                    chatId?.let { navigator.goTo(ChatConfigurationScreen(it)) }
                }

                ChatUiEvent.Ui.ReloadClicked -> {
                    // Dismissed regardless of what retry finds: an error about a run that is over
                    // would otherwise be a screen with no way off it. When there *is* something to
                    // run, the queued run reports `Working` and the timeline takes over from here.
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
                isThinking = currentState.execution is ChatExecutionState.Working,
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

/**
 * Which rendered item a search result meant.
 *
 * A turn projects reasoning *before* the reply it produced, and both carry the same message
 * sequence — so taking the first match would tint a collapsed thought bubble rather than the text
 * that was searched. Messages win; anything else with that sequence is the fallback, so a hit is
 * still shown rather than silently ignored.
 *
 * Null when nothing carries the sequence: the message may have been rewritten by history
 * compression, or the timeline may have been restored from a checkpoint whose indices do not line
 * up. The screen then opens normally, which is the right answer either way.
 */
internal fun resolveHighlightedItemId(items: List<ChatItem>, messageSequence: Long?): String? {
    if (messageSequence == null) return null

    val candidates = items.filter { it.messageSequence == messageSequence }
    val message = candidates.firstOrNull {
        it is ChatItem.UserMessage || it is ChatItem.AssistantMessage
    }
    return (message ?: candidates.firstOrNull())?.id
}
