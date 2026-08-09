package com.mvlog.chat.presentation.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.File
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Text
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.panelColor
import com.composeunstyled.theme.Theme
import com.mvlog.chat.presentation.ui.model.ChatAttachmentUi
import com.mvlog.ui.ThoonPreview

@Composable
fun ChatFileAttachment(
    attachment: ChatAttachmentUi,
    modifier: Modifier = Modifier,
) {
    val icon = when(attachment.type) {
        ChatAttachmentUi.Type.IMAGE -> Lucide.Image
        ChatAttachmentUi.Type.TEXT_FILE -> Lucide.Text
        ChatAttachmentUi.Type.BINARY -> Lucide.File
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Theme[colors][panelColor])
            .padding(10.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, modifier = Modifier.width(30.dp).height(30.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = attachment.name, modifier = Modifier, maxLines = 1)
    }
}

@Composable
@Preview
fun ChatAttachmentsPreview() {
    ThoonPreview {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ChatFileAttachment(attachment = ChatAttachmentUi(name = "username.txt", type = ChatAttachmentUi.Type.TEXT_FILE))
            ChatFileAttachment(attachment = ChatAttachmentUi(name = "exec.bin", type = ChatAttachmentUi.Type.BINARY))
            ChatFileAttachment(attachment = ChatAttachmentUi(name = "Anapa2007.png", type = ChatAttachmentUi.Type.IMAGE))

        }
    }
}
