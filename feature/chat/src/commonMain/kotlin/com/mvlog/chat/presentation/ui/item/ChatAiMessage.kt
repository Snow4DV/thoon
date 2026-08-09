package com.mvlog.chat.presentation.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.ui.theme.colors
import com.composables.ui.theme.panelColor
import com.composeunstyled.theme.Theme
import com.mvlog.chat.presentation.ui.model.ChatAttachmentUi
import com.mvlog.markdown.ThoonMarkdown
import com.mvlog.ui.ThoonPreview
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

@Composable
fun ChatAiMessage(
    contentMarkdown: String,
    createdAt: Instant,
    attachments: PersistentList<ChatAttachmentUi>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ThoonMarkdown(content = contentMarkdown, isOnPanel = false, modifier = Modifier)
    }
}


@Composable
fun ChatUserMessage(
    contentMarkdown: String,
    createdAt: Instant,
    attachments: PersistentList<ChatAttachmentUi>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Theme[colors][panelColor])
            .widthIn(min = 150.dp)
            .heightIn(min = 50.dp)
        ) {

        }
    }
}

@Preview
@Composable
fun ChatAiMessagePreview() {
    ThoonPreview {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChatAiMessage(
                contentMarkdown = "Hello *world*!",
                createdAt = Instant.fromEpochSeconds(312311),
                attachments = persistentListOf(ChatAttachmentUi(name = "code.txt", type = ChatAttachmentUi.Type.TEXT_FILE))
            )
            ChatUserMessage(
                contentMarkdown = "Hello *world*!",
                createdAt = Instant.fromEpochSeconds(312311),
                attachments = persistentListOf(ChatAttachmentUi(name = "code.txt", type = ChatAttachmentUi.Type.TEXT_FILE))
            )
        }
    }
}
