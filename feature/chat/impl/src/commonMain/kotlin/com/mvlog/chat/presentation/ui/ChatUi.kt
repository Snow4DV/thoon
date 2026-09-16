package com.mvlog.chat.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.CloudOff
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.RefreshCw
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.primaryColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.chat.presentation.ChatUiEvent
import com.mvlog.chat.presentation.ChatUiState
import com.mvlog.chat.presentation.item.ChatItem
import com.mvlog.chat.presentation.ui.component.ChatUiPromptField
import com.mvlog.chat.presentation.ui.item.ChatAiMessage
import com.mvlog.chat.presentation.ui.item.ChatAiThought
import com.mvlog.chat.presentation.ui.item.ChatToolChainCall
import com.mvlog.chat.presentation.ui.item.ChatUserMessage
import com.mvlog.chat.presentation.ui.item.shimmer.ChatAiMessageShimmer
import com.mvlog.chat.presentation.ui.item.shimmer.ChatAiThoughtShimmer
import com.mvlog.chat.presentation.ui.item.shimmer.ChatToolChainCallShimmer
import com.mvlog.chat.presentation.ui.item.shimmer.ChatUserMessageShimmer
import com.mvlog.chat.presentation.ui.model.ChatAttachmentUi
import com.mvlog.ui.ThoonPreview
import com.mvlog.ui.ThoonTypography
import com.mvlog.ui.components.ThoonTopBar
import com.mvlog.ui.shimmer.TextShimmer
import kotlinx.coroutines.flow.filter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.time.Instant

@Composable
fun ChatUi(
    state: ChatUiState,
    modifier: Modifier = Modifier,
) {
    // Insets on the field, not the column, or the keyboard lifts the top bar; imePadding first so
    // the two do not stack.
    val promptFieldModifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .navigationBarsPadding()
        .padding(10.dp)

    Column(modifier = modifier) {
        ChatUiHeader(state)
        ChatUiContent(
            state = state,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        when (state) {
            is ChatUiState.Data -> ChatUiPromptField(
                initialPrompt = state.prompt,
                onPromptChanged = { state.eventSink(ChatUiEvent.Ui.PromptChanged(it)) },
                onSubmit = { state.eventSink(ChatUiEvent.Ui.PromptSubmitted(it)) },
                modifier = promptFieldModifier,
            )

            is ChatUiState.Loading -> ChatUiPromptField(
                initialPrompt = "",
                onPromptChanged = {},
                isLoading = true,
                modifier = promptFieldModifier,
            )

            is ChatUiState.Error -> Unit
        }
    }
}

@Composable
private fun ChatUiHeader(
    state: ChatUiState,
    modifier: Modifier = Modifier,
) {
    // Anchored to the whole bar: ThoonTopBar has a click lambda, not an anchor slot.
    DropdownMenu(
        expanded = state.isChatOptionsMenuVisible,
        onExpandedChange = { expanded ->
            if (!expanded) state.eventSink(ChatUiEvent.Ui.ChatOptionsDismissed)
        },
        alignment = DropdownMenuAlignment.End,
        panel = {
            DropdownMenuPanel {
                DropdownMenuItem(onClick = { state.eventSink(ChatUiEvent.Ui.ChatSettingsClicked) }) {
                    Text("Chat settings")
                }
            }
        },
        anchor = {
            ThoonTopBar(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Theme[colors][secondaryColor])
                    .statusBarsPadding(),
                title = {
                    state.chatTitle?.let { chatTitle ->
                        Text(chatTitle, fontSize = 20.sp)
                    } ?: run {
                        TextShimmer(
                            textLength = 15,
                            fontSize = 20.sp,
                        )
                    }
                },
                onBackClicked = { state.eventSink(ChatUiEvent.Ui.GoBackClicked) },
                onOptionsClick = { state.eventSink(ChatUiEvent.Ui.OpenChatOptionsClicked) },
            )
        },
    )
}

@Composable
private fun ChatUiContent(
    state: ChatUiState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ChatUiState.Data -> ChatUiDataContent(state = state, modifier = modifier)
        is ChatUiState.Error -> ChatUiErrorContent(state = state, modifier = modifier)
        is ChatUiState.Loading -> ChatUiLoadingContent(modifier = modifier)
    }
}

@Composable
private fun ChatUiDataContent(
    state: ChatUiState.Data,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    var isFollowing by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect { isFollowing = listState.firstVisibleItemIndex == 0 }
    }

    var isJumpPending by remember { mutableStateOf(state.isDeepLinked) }
    var isTinted by remember { mutableStateOf(false) }

    LaunchedEffect(state.items.lastOrNull()?.id) {
        if (!isJumpPending && isFollowing) listState.scrollToItem(0)
    }

    // Keyed on size: the list's identity changes every streaming frame.
    LaunchedEffect(state.highlightedItemId, state.items.size) {
        if (!isJumpPending || state.items.isEmpty()) return@LaunchedEffect

        val index = state.highlightedItemId?.let { id -> state.items.indexOfFirst { it.id == id } }
            ?: -1

        // Cleared even on a miss, or following stays off for the rest of the visit.
        isJumpPending = false
        if (index < 0) return@LaunchedEffect

        listState.scrollToItem(state.items.lastIndex - index)
        isTinted = true
        delay(HIGHLIGHT_MILLIS)
        isTinted = false
    }

    // spacedBy stays Alignment.Top on purpose: reverseLayout inverts it, so Top rests a short chat
    // on the prompt field.
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(10.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        items(state.items.asReversed(), key = { it.id }) { chatItem ->
            val tint by animateColorAsState(
                targetValue = if (isTinted && chatItem.id == state.highlightedItemId) {
                    Theme[colors][primaryColor].copy(alpha = HIGHLIGHT_ALPHA)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(durationMillis = HIGHLIGHT_FADE_MILLIS),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint)
                    .padding(4.dp),
            ) {
                when (chatItem) {
                    is ChatItem.Message -> when (chatItem.origin) {
                        ChatItem.Message.Origin.AI -> ChatAiMessage(
                            contentMarkdown = chatItem.contentMarkdown,
                            createdAt = null,
                            attachments = chatItem.attachments.map { it.toUi() }.toPersistentList(),
                        )

                        ChatItem.Message.Origin.USER -> ChatUserMessage(
                            contentMarkdown = chatItem.contentMarkdown,
                            createdAt = null,
                            attachments = chatItem.attachments.map { it.toUi() }.toPersistentList(),
                        )
                    }

                    is ChatItem.Thought -> ChatAiThought(
                        isThinking = state.isThinking && chatItem.id == state.items.lastOrNull()?.id,
                        thoughts = chatItem.thoughts.toPersistentList(),
                        isExpanded = chatItem.isExpanded,
                        onExpandedChange = {
                            state.eventSink(ChatUiEvent.Ui.ThoughtExpandedChanged(chatItem.id, it))
                        },
                    )

                    is ChatItem.ToolChainCall -> ChatToolChainCall(
                        toolName = chatItem.toolName,
                        action = chatItem.action,
                        status = chatItem.status,
                        isExpanded = chatItem.isExpanded,
                        onExpandedChange = {
                            state.eventSink(ChatUiEvent.Ui.ToolChainCallExpandedChanged(chatItem.id, it))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatUiErrorContent(
    state: ChatUiState.Error,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(15.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Lucide.CloudOff,
            tint = Theme[colors][primaryColor],
            modifier = Modifier.width(60.dp).height(60.dp)
        )
        Text("Failed to load :(", style = ThoonTypography.h1, color = Theme[colors][primaryColor])
        Text(state.description, style = ThoonTypography.h3, color = Theme[colors][mutedColor])

        Button(
            style = ButtonStyle.Primary,
            onClick = { state.eventSink(ChatUiEvent.Ui.ReloadClicked) },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Lucide.RefreshCw, modifier = Modifier.size(16.dp))
                Text("Retry")
            }
        }
        Button(
            style = ButtonStyle.Outlined,
            onClick = { state.eventSink(ChatUiEvent.Ui.OpenSettingsClicked) },
        ) {
            Text("Open chat settings")
        }
    }
}

@Composable
private fun ChatUiLoadingContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        repeat(5) {
            ChatUserMessageShimmer()
            ChatAiMessageShimmer()
            ChatAiThoughtShimmer()
            ChatToolChainCallShimmer()
            ChatAiThoughtShimmer()
            ChatAiMessageShimmer()
        }
    }
}

private fun ChatItem.Message.Attachment.toUi(): ChatAttachmentUi = when (this) {
    is ChatItem.Message.Attachment.Text -> ChatAttachmentUi(name = name, type = ChatAttachmentUi.Type.TEXT_FILE)
    is ChatItem.Message.Attachment.Image -> ChatAttachmentUi(name = name, type = ChatAttachmentUi.Type.IMAGE)
    is ChatItem.Message.Attachment.Binary -> ChatAttachmentUi(name = name, type = ChatAttachmentUi.Type.BINARY)
}

@Preview
@Composable
private fun ChatUiLoadedPreview() {
    ThoonPreview {
        ChatUi(
            state = ChatUiState.Data(
                chatTitle = "Weather on a trip",
                items = persistentListOf(
                    ChatItem.Message(
                        id = "1",
                        contentMarkdown = "What weather will be like on a trip from Moscow to Saint Petersburg tomorrow if i leave at 9 AM?",
                        createdAt = Instant.fromEpochSeconds(0),
                        origin = ChatItem.Message.Origin.USER,
                    ),
                    ChatItem.Message(
                        id = "2",
                        contentMarkdown = """
                        I will have a look at a route from Moscow to Saint Petersburg, Russia on 9th of August, 2026 and will tell you
                        a weather on the way there
                        """.trimIndent(),
                        createdAt = Instant.fromEpochSeconds(1),
                        origin = ChatItem.Message.Origin.AI,
                    ),
                    ChatItem.Thought(
                        id = "3",
                        thoughts = listOf("I will use OsmAnd agent to see the route and points on the way"),
                        createdAt = Instant.fromEpochSeconds(2),
                    ),
                    ChatItem.ToolChainCall(
                        id = "4",
                        createdAt = Instant.fromEpochSeconds(3),
                        toolName = "OsmAnd",
                        action = "Get route from Moscow to Saint Petersburg",
                        status = ChatItem.ToolChainCall.Status.Success("Obtained route + ETA successfully")
                    ),
                    ChatItem.Thought(
                        id = "5",
                        thoughts = listOf("I will use AccuWeather agent to see the weather in 30 points on the way during the drive with ETA for each point"),
                        createdAt = Instant.fromEpochSeconds(4),
                    ),
                    ChatItem.ToolChainCall(
                        id = "6",
                        createdAt = Instant.fromEpochSeconds(5),
                        toolName = "AccuWeather",
                        action = "Get weather at 30 points of a route from Moscow to Saint Petersburg",
                        status = ChatItem.ToolChainCall.Status.Success("Obtained 30 weather data points")
                    ),
                    ChatItem.Thought(
                        id = "7",
                        thoughts = listOf(
                            "Creating the answer for the user",
                            "I should include everything that driver should be aware of - where will it be cold, where will it rain, summary of the weather"
                        ),
                        createdAt = Instant.fromEpochSeconds(6),
                    ),
                    ChatItem.Message(
                        id = "8",
                        contentMarkdown = """
                        Overall the weather will be good and temperature will be around 21 degrees, but it is going to rain near Okulovka on the 4th hour of your trip.
                        You should take the umbrella with out and refill the tank with some fresh washer fluid! 
                        """.trimIndent(),
                        createdAt = Instant.fromEpochSeconds(7),
                        origin = ChatItem.Message.Origin.AI,
                    ),
                ),
                prompt = "Also look if all gas stations on the way here have gas at the moment please",
                highlightedItemId = null,
                isDeepLinked = false,
                isThinking = false,
                isRefreshing = false,
                isChatOptionsMenuVisible = false,
                eventSink = {},
            )
        )
    }
}


@Preview
@Composable
fun ChatUiLoadingPreview() {
    ThoonPreview {
        ChatUi(
            state = ChatUiState.Loading(
                chatTitle = null,
                eventSink = {},
                isChatOptionsMenuVisible = false,
            )
        )
    }
}

@Preview
@Composable
fun ChatUiErrorPreview() {
    ThoonPreview {
        ChatUi(
            state = ChatUiState.Error(
                description = "Connection to server timed out",
                chatTitle = null,
                eventSink = {},
                isChatOptionsMenuVisible = false
            )
        )
    }
}

private const val HIGHLIGHT_MILLIS = 2000L

private const val HIGHLIGHT_FADE_MILLIS = 400

private const val HIGHLIGHT_ALPHA = 0.22f
