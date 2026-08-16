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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Loader
import com.composables.icons.lucide.Lucide
import com.composables.ui.components.Disclosure
import com.composables.ui.components.DisclosurePanel
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composeunstyled.UnstyledDisclosureButton
import com.composeunstyled.theme.Theme
import com.mvlog.ui.ThoonPreview
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Expandable thought
 */
@Composable
fun ChatAiThought(
    isThinking: Boolean,
    thoughts: PersistentList<String>,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    formattedDuration: String? = null,
) {
    val mutedContentColor = Theme[colors][mutedColor]

    Disclosure(
        expanded = isExpanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        UnstyledDisclosureButton(contentPadding = PaddingValues(0.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val rotationAngle = if (isThinking) {
                    val transition = rememberInfiniteTransition(label = "thinking")
                    val angle by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = LinearEasing),
                        ),
                        label = "thinking_rotation",
                    )
                    angle
                } else {
                    0f
                }

                Icon(
                    imageVector = if (isThinking) Lucide.Loader else Lucide.Lightbulb,
                    tint = mutedContentColor,
                    modifier = Modifier.size(16.dp).rotate(rotationAngle),
                )
                Text(
                    text = when {
                        isThinking -> "Thinking…"
                        formattedDuration != null -> "Thought for $formattedDuration"
                        else -> "Thought"
                    },
                    color = mutedContentColor,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Lucide.ChevronUp else Lucide.ChevronDown,
                    tint = mutedContentColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        DisclosurePanel(contentPadding = PaddingValues(top = 5.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                thoughts.forEach { thought ->
                    Row {
                        Text(text = "•", color = mutedContentColor, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = thought, color = mutedContentColor, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ChatAiThoughtPreview() {
    ThoonPreview {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            var thinkingExpanded by remember { mutableStateOf(true) }
            ChatAiThought(
                isThinking = true,
                thoughts = persistentListOf(
                    "Checking the current route from Moscow to Saint Petersburg",
                    "Looking up the weather along the way",
                ),
                isExpanded = thinkingExpanded,
                onExpandedChange = { thinkingExpanded = it },
            )

            var collapsedExpanded by remember { mutableStateOf(false) }
            ChatAiThought(
                isThinking = false,
                formattedDuration = "4s",
                thoughts = persistentListOf(
                    "Checked the M11 route status",
                    "Cross-referenced AccuWeather for the 4th hour of the trip",
                    "Confirmed gas stations are open along the way",
                ),
                isExpanded = collapsedExpanded,
                onExpandedChange = { collapsedExpanded = it },
            )

            var expandedExpanded by remember { mutableStateOf(true) }
            ChatAiThought(
                isThinking = false,
                formattedDuration = "1m 12s",
                thoughts = persistentListOf(
                    "Checked the M11 route status",
                    "Cross-referenced AccuWeather for the 4th hour of the trip",
                    "Confirmed gas stations are open along the way",
                ),
                isExpanded = expandedExpanded,
                onExpandedChange = { expandedExpanded = it },
            )
        }
    }
}
