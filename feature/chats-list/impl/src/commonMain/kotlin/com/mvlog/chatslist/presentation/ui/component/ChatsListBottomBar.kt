package com.mvlog.chatslist.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Search
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.NavigationBarItem
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.onPrimaryColor
import com.composables.ui.theme.panelColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.LocalContentColor
import com.composeunstyled.theme.Theme
import com.mvlog.chatslist.presentation.ChatsListTab

@Composable
internal fun ChatsListBottomBar(
    selected: ChatsListTab,
    onTabSelected: (ChatsListTab) -> Unit,
    onNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Not the library NavigationBar: that is edge-to-edge with a separator; this is a floating
    // pill.
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(Theme[colors][panelColor])
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab(
            isSelected = selected == ChatsListTab.Chats,
            onClick = { onTabSelected(ChatsListTab.Chats) },
        ) {
            Icon(Lucide.MessageCircle, contentDescription = "Chats")
        }

        Tab(
            isSelected = selected == ChatsListTab.Search,
            onClick = { onTabSelected(ChatsListTab.Search) },
        ) {
            Icon(Lucide.Search, contentDescription = "Search")
        }

        IconButton(onClick = onNewChat, style = ButtonStyle.Primary) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = "New chat",
                tint = Theme[colors][onPrimaryColor],
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun Tab(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    // NavigationBarItem colours only its background on selection; content tint goes through
    // LocalContentColor.
    val tint = if (isSelected) Theme[colors][primaryColor] else Theme[colors][mutedColor]

    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
    ) {
        CompositionLocalProvider(LocalContentColor provides tint, content = content)
    }
}
