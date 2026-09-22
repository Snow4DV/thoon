package com.mvlog.chat.presentation.ui.item

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.CircleX
import com.composables.icons.lucide.Loader
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldQuestion
import com.composables.icons.lucide.Wrench
import com.composables.ui.components.Disclosure
import com.composables.ui.components.DisclosurePanel
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.UnstyledDisclosureButton
import com.composeunstyled.theme.Theme
import com.mvlog.chat.presentation.item.ChatItem
import com.mvlog.ui.ThoonPreview

@Composable
fun ChatToolChainCall(
    toolName: String,
    action: String,
    status: ChatItem.ToolChainCall.Status,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mutedContentColor = Theme[colors][mutedColor]
    val canExpand = status !is ChatItem.ToolChainCall.Status.Loading

    Disclosure(
        expanded = isExpanded && canExpand,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        UnstyledDisclosureButton(
            enabled = canExpand,
            contentPadding = PaddingValues(0.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Lucide.Wrench,
                    tint = mutedContentColor,
                    modifier = Modifier.size(16.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = toolName,
                        color = mutedContentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = action,
                        color = mutedContentColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ChatToolChainCallStatusIcon(status = status)
                if (canExpand) {
                    Icon(
                        imageVector = if (isExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                        tint = mutedContentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (canExpand) {
            DisclosurePanel(contentPadding = PaddingValues(0.dp)) {
                when (status) {
                    is ChatItem.ToolChainCall.Status.Success -> Text(
                        text = status.result,
                        color = mutedContentColor,
                        fontSize = 13.sp,
                    )

                    is ChatItem.ToolChainCall.Status.Failure -> Text(
                        text = status.errorMessage,
                        color = Theme[colors][destructiveColor],
                        fontSize = 13.sp,
                    )

                    ChatItem.ToolChainCall.Status.AwaitingApproval -> Text(
                        text = action,
                        color = mutedContentColor,
                        fontSize = 13.sp,
                    )

                    ChatItem.ToolChainCall.Status.Loading -> Unit
                }
            }
        }
    }
}

@Composable
private fun ChatToolChainCallStatusIcon(status: ChatItem.ToolChainCall.Status) {
    when (status) {
        ChatItem.ToolChainCall.Status.Loading -> {
            val mutedContentColor = Theme[colors][mutedColor]
            val transition = rememberInfiniteTransition(label = "tool_call_loading")
            val angle by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                ),
                label = "tool_call_loading_rotation",
            )
            Icon(
                imageVector = Lucide.Loader,
                tint = mutedContentColor,
                modifier = Modifier.size(16.dp).rotate(angle),
            )
        }

        ChatItem.ToolChainCall.Status.AwaitingApproval -> Icon(
            imageVector = Lucide.ShieldQuestion,
            tint = Theme[colors][primaryColor],
            modifier = Modifier.size(16.dp),
        )

        is ChatItem.ToolChainCall.Status.Success -> Icon(
            imageVector = Lucide.CircleCheck,
            tint = Theme[colors][primaryColor],
            modifier = Modifier.size(16.dp),
        )

        is ChatItem.ToolChainCall.Status.Failure -> Icon(
            imageVector = Lucide.CircleX,
            tint = Theme[colors][destructiveColor],
            modifier = Modifier.size(16.dp),
        )
    }
}

@Preview
@Composable
fun ChatToolChainCallPreview() {
    ThoonPreview {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChatToolChainCall(
                toolName = "OsmAnd",
                action = "Get route from Moscow to Saint Petersburg",
                status = ChatItem.ToolChainCall.Status.Loading,
                isExpanded = false,
                onExpandedChange = {},
            )

            var successExpanded by remember { mutableStateOf(true) }
            ChatToolChainCall(
                toolName = "OsmAnd",
                action = "Get route from Moscow to Saint Petersburg",
                status = ChatItem.ToolChainCall.Status.Success("Obtained route + ETA successfully"),
                isExpanded = successExpanded,
                onExpandedChange = { successExpanded = it },
            )

            var failureExpanded by remember { mutableStateOf(true) }
            ChatToolChainCall(
                toolName = "AccuWeather",
                action = "Get weather at 30 points along the route",
                status = ChatItem.ToolChainCall.Status.Failure("Request timed out after 3 retries"),
                isExpanded = failureExpanded,
                onExpandedChange = { failureExpanded = it },
            )
        }
    }
}
