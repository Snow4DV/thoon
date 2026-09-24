package com.mvlog.chat.presentation.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.File
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Text
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.mvlog.chat.presentation.ui.model.ChatAttachmentUi
import com.mvlog.ui.ThoonPreview
import com.mvlog.ui.components.panel

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
            .panel(RoundedCornerShape(10.dp))
            .padding(10.dp)
            .widthIn(max = 150.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, modifier = Modifier.width(25.dp).height(25.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = attachment.name, modifier = Modifier, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(fontSize = 16.sp))
    }
}

@Composable
@Preview
fun ChatAttachmentsPreview() {
    ThoonPreview {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(5.dp)) {
            ChatFileAttachment(attachment = ChatAttachmentUi(name = "username.txt", type = ChatAttachmentUi.Type.TEXT_FILE))
            ChatFileAttachment(attachment = ChatAttachmentUi(name = "exec.bin", type = ChatAttachmentUi.Type.BINARY))
            ChatFileAttachment(attachment = ChatAttachmentUi(name = "Anapa2007.png", type = ChatAttachmentUi.Type.IMAGE))

        }
    }
}
