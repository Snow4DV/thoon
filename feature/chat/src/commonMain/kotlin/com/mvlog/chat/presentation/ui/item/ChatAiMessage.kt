package com.mvlog.chat.presentation.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
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
    createdAt: String?,
    attachments: PersistentList<ChatAttachmentUi>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (attachments.isNotEmpty()) {
            val scrollState = rememberScrollState()
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.horizontalScroll(scrollState),
            ) {
                attachments.forEach {
                    ChatFileAttachment(attachment = it)
                }
            }
        }
        ThoonMarkdown(content = contentMarkdown, isOnPanel = false, modifier = Modifier)
        createdAt?.let {
            Text(
                text = it, color = Theme[colors][mutedColor],
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}


@Composable
fun ChatUserMessage(
    contentMarkdown: String,
    createdAt: String?,
    attachments: PersistentList<ChatAttachmentUi>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopEnd,
    ) {
        val maxMessageWidth = maxWidth * 0.7f
        Column(
            modifier = Modifier.widthIn(max = maxMessageWidth),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (attachments.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.horizontalScroll(scrollState),
                ) {
                    attachments.forEach {
                        ChatFileAttachment(attachment = it)
                    }
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Theme[colors][panelColor])
                    .widthIn(min = 150.dp)
                    .heightIn(min = 50.dp)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    ThoonMarkdown(content = contentMarkdown, isOnPanel = true)
                    createdAt?.let {
                        Text(
                            text = it, color = Theme[colors][mutedColor],
                            modifier = Modifier.align(Alignment.End),
                        )
                    }
                }
            }
        }
    }
}

private fun sampleAiAnswer(): String = """
    ## Route overview

    Here's what I found for the drive from Moscow to Saint Petersburg tomorrow morning:

    - Distance: **714 km**, about 8h 40m without stops
    - Weather: clear until Novgorod, light rain near Okulovka around hour 4
    - Fuel: gas stations are open the entire way along the M11

    You can pull the same summary programmatically with:

    ```kotlin
    val trip = Trip.plan(from = "Moscow", to = "Saint Petersburg")
    println(trip.eta)
    ```

    Let me know if you'd like the return trip planned too.
""".trimIndent()

@Preview
@Composable
fun ChatAiMessagePreview() {
    ThoonPreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(5.dp)
        ) {
            ChatUserMessage(
                contentMarkdown = "What's the fastest route from Moscow to Saint Petersburg tomorrow morning, and will I need an umbrella?",
                createdAt = "12:04",
                attachments = persistentListOf(
                    ChatAttachmentUi(
                        name = "example_very_long_file_name_that_should_be.png",
                        type = ChatAttachmentUi.Type.IMAGE
                    ),
                    ChatAttachmentUi(
                        name = "other.bin",
                        type = ChatAttachmentUi.Type.BINARY
                    )
                ),
            )
            ChatAiMessage(
                contentMarkdown = sampleAiAnswer(),
                createdAt = "12:05",
                attachments = persistentListOf(
                    ChatAttachmentUi(
                        name = "route.gpx",
                        type = ChatAttachmentUi.Type.TEXT_FILE
                    )
                ),
            )
        }
    }
}
