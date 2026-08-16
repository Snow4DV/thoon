package com.mvlog.chat.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mvlog.agent.api.model.ChatExecutionState
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.usecase.CancelAgentRunUseCase
import com.mvlog.agent.api.usecase.CreateChatUseCase
import com.mvlog.agent.api.usecase.ObserveChatUseCase
import com.mvlog.agent.api.usecase.SendPromptUseCase
import com.mvlog.chat.presentation.mapper.toUi
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
    private val observeChat: ObserveChatUseCase,
    private val createChat: CreateChatUseCase,
    private val sendPrompt: SendPromptUseCase,
    private val cancelAgentRun: CancelAgentRunUseCase,
    private val chatTitle: String = DEFAULT_TITLE,
) : Presenter<ChatScreen.State> {

    @Composable
    override fun present(): ChatScreen.State {
        val scope = rememberCoroutineScope()

        var prompt by remember { mutableStateOf("") }
        var expandedIds by remember { mutableStateOf(emptySet<String>()) }
        var isChatOptionsMenuVisible by remember { mutableStateOf(false) }
        var failure by remember { mutableStateOf<String?>(null) }

        val chatId by produceState<ChatId?>(initialValue = null) {
            value = createChat(configId = null)
        }

        val chatState by produceState<ChatState?>(initialValue = null, chatId) {
            val id = chatId ?: return@produceState
            observeChat(id).collectLatest { value = it }
        }

        // Surface a failed run once, as a screen-level error, then let the timeline stand.
        LaunchedEffect(chatState?.execution) {
            failure = (chatState?.execution as? ChatExecutionState.Failed)?.message
        }

        val eventSink: (ChatScreen.Event) -> Unit = { event ->
            when (event) {
                is ChatScreen.Event.Ui.PromptChanged -> prompt = event.prompt

                is ChatScreen.Event.Ui.PromptSubmitted -> {
                    val id = chatId
                    if (id != null && event.prompt.isNotBlank()) {
                        prompt = ""
                        // Fire-and-forget: `prompt` returns once the run is durable, and the answer
                        // arrives through the observed state rather than through this call.
                        scope.launch { sendPrompt(chatId = id, text = event.prompt) }
                    }
                }

                is ChatScreen.Event.Ui.CancelGenerationClicked -> {
                    val working = chatState?.execution as? ChatExecutionState.Working
                    if (working != null) {
                        scope.launch { cancelAgentRun(working.runId) }
                    }
                }

                is ChatScreen.Event.Ui.ThoughtExpandedChanged ->
                    expandedIds = expandedIds.toggle(event.id, event.isExpanded)

                is ChatScreen.Event.Ui.ToolChainCallExpandedChanged ->
                    expandedIds = expandedIds.toggle(event.id, event.isExpanded)

                ChatScreen.Event.Ui.OpenChatOptionsClicked ->
                    isChatOptionsMenuVisible = !isChatOptionsMenuVisible

                ChatScreen.Event.Ui.ReloadClicked -> failure = null

                ChatScreen.Event.Ui.GoBackClicked -> Unit
            }
        }

        val currentFailure = failure
        val currentState = chatState

        return when {
            currentFailure != null -> ChatScreen.State.Error(
                description = currentFailure,
                chatTitle = chatTitle,
                eventSink = eventSink,
                isChatOptionsMenuVisible = isChatOptionsMenuVisible,
            )

            currentState == null -> ChatScreen.State.Loading(
                chatTitle = chatTitle,
                eventSink = eventSink,
                isChatOptionsMenuVisible = isChatOptionsMenuVisible,
            )

            else -> ChatScreen.State.Data(
                items = currentState.items.map { it.toUi(expandedIds) }.toPersistentList(),
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
