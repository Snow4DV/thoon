package com.mvlog.chatslist.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Trash2
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.chatslist.presentation.ChatRow
import com.mvlog.chatslist.presentation.ChatsListTab
import com.mvlog.chatslist.presentation.ChatsListUiEvent
import com.mvlog.chatslist.presentation.ChatsListUiState
import com.mvlog.chatslist.presentation.ui.component.ChatsListBottomBar
import com.mvlog.chatslist.presentation.ui.component.ChatsSearchField
import com.mvlog.ui.ThoonTypography
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.defaultShimmerTheme
import com.valentinilk.shimmer.rememberShimmer
import com.valentinilk.shimmer.shimmer

@Composable
fun ChatsListUi(
    state: ChatsListUiState,
    modifier: Modifier = Modifier,
) {
    // No top bar to own the status inset, so the screen takes it.
    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        when (state) {
            is ChatsListUiState.Loading -> Unit

            is ChatsListUiState.Data -> {
                ChatsListDataContent(state)

                ChatsListBottomBar(
                    selected = state.tab,
                    onTabSelected = { state.eventSink(ChatsListUiEvent.Ui.TabSelected(it)) },
                    onNewChat = { state.eventSink(ChatsListUiEvent.Ui.NewChatClicked) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Above the keyboard, or the way out of Search is hidden behind it.
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatsListDataContent(state: ChatsListUiState.Data) {
    Column(modifier = Modifier.fillMaxSize()) {
        Header(state)

        when (state.emptiness) {
            ChatsListUiState.Emptiness.None -> ChatList(state)

            ChatsListUiState.Emptiness.NoChats -> EmptyState(
                title = "No chats yet",
                action = {
                    Button(
                        onClick = { state.eventSink(ChatsListUiEvent.Ui.NewChatClicked) },
                        style = ButtonStyle.Primary,
                    ) {
                        Text("Create a new chat")
                    }
                },
            )

            ChatsListUiState.Emptiness.NoMatches -> EmptyState(
                title = "Nothing matches",
                subtitle = "No chat contains \"${state.query.trim()}\".",
            )
        }
    }
}

@Composable
private fun Header(state: ChatsListUiState.Data) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = "Chats",
                style = ThoonTypography.display,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = { state.eventSink(ChatsListUiEvent.Ui.SettingsClicked) },
                style = ButtonStyle.Ghost,
            ) {
                Icon(Lucide.Settings, contentDescription = "Settings")
            }
        }

        if (state.tab == ChatsListTab.Search) {
            ChatsSearchField(
                initialQuery = state.query,
                onQueryChanged = { state.eventSink(ChatsListUiEvent.Ui.QueryChanged(it)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun ColumnScope.ChatList(state: ChatsListUiState.Data) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f),
        // Clears the floating bar over the last row.
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        items(state.chats, key = { it.key }) { chat ->
            ChatListRow(
                chat = chat,
                query = state.query,
                onClick = {
                    state.eventSink(
                        ChatsListUiEvent.Ui.ChatClicked(chat.id, chat.messageSequence)
                    )
                },
                onStop = { state.eventSink(ChatsListUiEvent.Ui.StopClicked(chat.id)) },
                onDelete = { state.eventSink(ChatsListUiEvent.Ui.DeleteClicked(chat.id)) },
            )
        }
    }
}

@Composable
private fun ChatListRow(
    chat: ChatRow,
    query: String,
    onClick: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
) {
    val matchStyle = SpanStyle(
        background = Theme[colors][primaryColor].copy(alpha = HIGHLIGHT_ALPHA),
        fontWeight = FontWeight.Medium,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = highlight(chat.label, query, matchStyle),
                modifier = if (chat.isWorking) Modifier.shimmer(rememberWorkingShimmer()) else Modifier,
                style = ThoonTypography.h5,
                singleLine = true,
                overflow = TextOverflow.Ellipsis,
            )
            chat.subtitle?.let {
                Text(
                    text = highlight(it, query, matchStyle),
                    style = ThoonTypography.caption,
                    color = Theme[colors][mutedColor],
                    singleLine = true,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (chat.isWorking) {
            IconButton(onClick = onStop, style = ButtonStyle.Ghost) {
                Icon(Lucide.Square, contentDescription = "Stop agent")
            }
        }

        IconButton(onClick = onDelete, style = ButtonStyle.Ghost) {
            Icon(Lucide.Trash2, contentDescription = "Delete chat")
        }
    }
}

@Composable
private fun rememberWorkingShimmer() = rememberShimmer(
    shimmerBounds = ShimmerBounds.View,
    theme = defaultShimmerTheme.copy(
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = WORKING_SHIMMER_SWEEP_MILLIS,
                delayMillis = WORKING_SHIMMER_PAUSE_MILLIS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
    ),
)

private const val WORKING_SHIMMER_SWEEP_MILLIS = 800
private const val WORKING_SHIMMER_PAUSE_MILLIS = 300

@Composable
private fun ColumnScope.EmptyState(
    title: String,
    subtitle: String? = null,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text = title, style = ThoonTypography.h1, textAlign = TextAlign.Center)
        subtitle?.let {
            Text(
                text = it,
                style = ThoonTypography.body,
                color = Theme[colors][mutedColor],
                textAlign = TextAlign.Center,
            )
        }
        action()
    }
}

private const val HIGHLIGHT_ALPHA = 0.25f
