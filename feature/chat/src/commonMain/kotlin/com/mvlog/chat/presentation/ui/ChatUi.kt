package com.mvlog.chat.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.composables.ui.theme.colors
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.chat.presentation.ChatScreen
import com.mvlog.chat.presentation.item.ChatItem
import com.mvlog.ui.ThoonPreview
import com.mvlog.ui.components.ThoonTopBar
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

@Composable
fun ChatUi(
    state: ChatScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ChatScreen.State.Data -> ChatUiLoaded(state, modifier)
        is ChatScreen.State.Error -> TODO()
        is ChatScreen.State.Loading -> TODO()
    }
}

@Composable
fun ChatUiLoaded(
    state: ChatScreen.State.Data,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        ThoonTopBar(
            modifier = Modifier.fillMaxWidth().background(Theme[colors][secondaryColor]),
            title = state.chatTitle,
            onBackClicked = { state.eventSink(ChatScreen.Event.Ui.GoBack) },
            onOptionsClick = { state.eventSink(ChatScreen.Event.Ui.OpenChatOptions) },
        )
    }
}
@Preview
@Composable
fun ChatUiLoadedPreview() {
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
