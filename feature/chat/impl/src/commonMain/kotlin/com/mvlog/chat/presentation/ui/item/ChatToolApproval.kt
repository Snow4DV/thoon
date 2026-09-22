package com.mvlog.chat.presentation.ui.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldQuestion
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenu
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.primaryColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalScope
import com.mvlog.ui.ThoonPreview

@Composable
fun ChatToolApproval(
    toolName: String,
    arguments: String,
    decision: ToolApprovalDecision?,
    onDecided: (ToolApprovalDecision) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mutedContentColor = Theme[colors][mutedColor]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Theme[colors][secondaryColor])
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Lucide.ShieldQuestion,
                tint = Theme[colors][primaryColor],
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Allow $toolName?",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (arguments.isNotBlank()) {
            Text(
                text = arguments,
                color = mutedContentColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        when (decision) {
            null -> ChatToolApprovalActions(onDecided = onDecided)

            is ToolApprovalDecision.Approve -> Text(
                text = "Allowed, waiting for the other calls",
                color = mutedContentColor,
                fontSize = 12.sp,
            )

            ToolApprovalDecision.Decline -> Text(
                text = "Declined",
                color = Theme[colors][destructiveColor],
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ChatToolApprovalActions(onDecided: (ToolApprovalDecision) -> Unit) {
    var isScopeMenuVisible by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            style = ButtonStyle.Primary,
            onClick = { onDecided(ToolApprovalDecision.Approve(ToolApprovalScope.Once)) },
        ) {
            Text("Allow")
        }

        DropdownMenu(
            expanded = isScopeMenuVisible,
            onExpandedChange = { isScopeMenuVisible = it },
            alignment = DropdownMenuAlignment.Start,
            panel = {
                DropdownMenuPanel {
                    DropdownMenuItem(
                        onClick = {
                            isScopeMenuVisible = false
                            onDecided(ToolApprovalDecision.Approve(ToolApprovalScope.Chat))
                        },
                    ) {
                        Text("For this chat")
                    }
                    DropdownMenuItem(
                        onClick = {
                            isScopeMenuVisible = false
                            onDecided(ToolApprovalDecision.Approve(ToolApprovalScope.Always))
                        },
                    ) {
                        Text("Everywhere")
                    }
                }
            },
            anchor = {
                Button(
                    style = ButtonStyle.Outlined,
                    onClick = { isScopeMenuVisible = true },
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Always allow")
                        Icon(imageVector = Lucide.ChevronDown, modifier = Modifier.size(14.dp))
                    }
                }
            },
        )

        Button(
            style = ButtonStyle.Ghost,
            onClick = { onDecided(ToolApprovalDecision.Decline) },
        ) {
            Text("Decline")
        }
    }
}

@Preview
@Composable
private fun ChatToolApprovalPreview() {
    ThoonPreview {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChatToolApproval(
                toolName = "write_file",
                arguments = """{"path":"notes.md","content":"# Trip"}""",
                decision = null,
                onDecided = {},
            )
            ChatToolApproval(
                toolName = "fetch_url",
                arguments = """{"url":"https://example.com"}""",
                decision = ToolApprovalDecision.Approve(ToolApprovalScope.Once),
                onDecided = {},
            )
            ChatToolApproval(
                toolName = "web_search",
                arguments = """{"query":"weather"}""",
                decision = ToolApprovalDecision.Decline,
                onDecided = {},
            )
        }
    }
}
