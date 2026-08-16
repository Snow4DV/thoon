package com.mvlog.chat.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.primaryColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.chat.presentation.ChatScreen
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
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.time.Instant

@Composable
fun ChatUi(
    state: ChatScreen.State,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ChatUiHeader(state)
        ChatUiContent(
            state = state,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        when (state) {
            is ChatScreen.State.Data -> ChatUiPromptField(
                initialPrompt = state.prompt,
                onPromptChanged = { state.eventSink(ChatScreen.Event.Ui.PromptChanged(it)) },
                onSubmit = { state.eventSink(ChatScreen.Event.Ui.PromptSubmitted(it)) },
                modifier = Modifier.fillMaxWidth().padding(10.dp),
            )

            is ChatScreen.State.Loading -> ChatUiPromptField(
                initialPrompt = "",
                onPromptChanged = {},
                isLoading = true,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
            )

            is ChatScreen.State.Error -> Unit
        }
    }
}

@Composable
private fun ChatUiHeader(
    state: ChatScreen.State,
    modifier: Modifier = Modifier,
) {
    ThoonTopBar(
            modifier = modifier.fillMaxWidth().background(Theme[colors][secondaryColor]),
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
            onBackClicked = { state.eventSink(ChatScreen.Event.Ui.GoBackClicked) },
            onOptionsClick = { state.eventSink(ChatScreen.Event.Ui.OpenChatOptionsClicked) },
        )
}

@Composable
private fun ChatUiContent(
    state: ChatScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ChatScreen.State.Data -> ChatUiDataContent(state = state, modifier = modifier)
        is ChatScreen.State.Error -> ChatUiErrorContent(state = state, modifier = modifier)
        is ChatScreen.State.Loading -> ChatUiLoadingContent(modifier = modifier)
    }
}

@Composable
private fun ChatUiDataContent(
    state: ChatScreen.State.Data,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(10.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        items(state.items, key = { it.id }) { chatItem ->
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
                    isThinking = state.isThinking && chatItem.id == state.items.last().id,
                    thoughts = chatItem.thoughts.toPersistentList(),
                    isExpanded = chatItem.isExpanded,
                    onExpandedChange = {
                        state.eventSink(ChatScreen.Event.Ui.ThoughtExpandedChanged(chatItem.id, it))
                    },
                )

                is ChatItem.ToolChainCall -> ChatToolChainCall(
                    toolName = chatItem.toolName,
                    action = chatItem.action,
                    status = chatItem.status,
                    isExpanded = chatItem.isExpanded,
                    onExpandedChange = {
                        state.eventSink(ChatScreen.Event.Ui.ToolChainCallExpandedChanged(chatItem.id, it))
                    },
                )
            }
        }
    }
}

@Composable
private fun ChatUiErrorContent(
    state: ChatScreen.State.Error,
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
            content = {
                Icon(Lucide.RefreshCw)
            },
            onClick = {
                state.eventSink(ChatScreen.Event.Ui.ReloadClicked)
            }
        )
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
            state = ChatScreen.State.Data(
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
            state = ChatScreen.State.Loading(
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
            state = ChatScreen.State.Error(
                description = "Connection to server timed out",
                chatTitle = null,
                eventSink = {},
                isChatOptionsMenuVisible = false
            )
        )
    }
}
