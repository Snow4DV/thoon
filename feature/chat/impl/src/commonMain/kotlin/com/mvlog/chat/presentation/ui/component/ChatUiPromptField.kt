package com.mvlog.chat.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.Square
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.composables.ui.components.TextField
import com.composables.ui.theme.colors
import com.composables.ui.theme.fieldShape
import com.composables.ui.theme.shapes
import com.composables.ui.theme.controlColor
import com.composables.ui.theme.fieldColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.onFieldColor
import com.composeunstyled.theme.Theme
import com.mvlog.ui.ThoonPreview
import com.mvlog.ui.components.workingBorder
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(FlowPreview::class)
@Composable
fun ChatUiPromptField(
    initialPrompt: String,
    onPromptChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    /** The agent is answering: the trailing button becomes stop and submit is refused. */
    isWorking: Boolean = false,
    /** A tool call waits on the user: nothing can be sent until it is answered. */
    isInputBlocked: Boolean = false,
    onSubmit: (String) -> Unit = {},
    onStop: () -> Unit = {},
) {
    val textFieldState = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState(initialPrompt) }
    val latestOnPromptChanged by rememberUpdatedState(onPromptChanged)
    val latestOnSubmit by rememberUpdatedState(onSubmit)

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .drop(1) // first emission is just initialPrompt echoed back; nothing changed yet
            .debounce(200)
            .collectLatest { latestOnPromptChanged(it) }
    }

    val canSend by remember(textFieldState) {
        derivedStateOf { textFieldState.text.isNotBlank() }
    }

    // Read from the field: the debounced callback can be behind. Shared by the button and the IME
    // action.
    fun submit() {
        // Refused, not queued: the draft stays in the field until the agent has stopped.
        if (isWorking || isInputBlocked) return
        val prompt = textFieldState.text.toString()
        if (prompt.isNotBlank()) {
            latestOnSubmit(prompt)
            textFieldState.clearText()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().let { if (isLoading) it.shimmer() else it },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        // Not in trailing: that slot top-aligns in a multi-line field.
        verticalAlignment = Alignment.Bottom,
    ) {
        TextField(
            state = textFieldState,
            modifier = Modifier
                .weight(1f)
                .workingBorder(active = isWorking, shape = Theme[shapes][fieldShape]),
            enabled = !isLoading,
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
            placeholder = {
                Text(
                    text = if (isInputBlocked) "Answer the tool request first…" else "Ask something…",
                    color = Theme[colors][mutedColor],
                )
            },
            backgroundColor = if (isLoading) Theme[colors][controlColor] else Theme[colors][fieldColor],
            contentColor = if (isLoading) Theme[colors][mutedColor] else Theme[colors][onFieldColor],
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            onKeyboardAction = KeyboardActionHandler { submit() },
        )

        when (val action = promptFieldAction(isWorking = isWorking, canSend = canSend && !isInputBlocked)) {
            PromptFieldAction.Stop -> IconButton(onClick = onStop, style = ButtonStyle.Primary) {
                Icon(Lucide.Square, contentDescription = "Stop")
            }

            PromptFieldAction.Send,
            PromptFieldAction.SendDisabled,
            -> IconButton(
                onClick = ::submit,
                enabled = !isLoading && action == PromptFieldAction.Send,
                style = ButtonStyle.Primary,
            ) {
                Icon(Lucide.Send, contentDescription = "Send")
            }
        }
    }
}

internal enum class PromptFieldAction { Stop, Send, SendDisabled }

/** Stop wins while the agent works; otherwise sending needs text. */
internal fun promptFieldAction(isWorking: Boolean, canSend: Boolean): PromptFieldAction = when {
    isWorking -> PromptFieldAction.Stop
    canSend -> PromptFieldAction.Send
    else -> PromptFieldAction.SendDisabled
}

@Preview
@Composable
private fun ChatUiPromptFieldPreview() {
    ThoonPreview {
        ChatUiPromptField(
            initialPrompt = "",
            onPromptChanged = {},
        )
    }
}

@Preview
@Composable
private fun ChatUiPromptFieldFilledPreview() {
    ThoonPreview {
        ChatUiPromptField(
            initialPrompt = "What is the fastest route from Moscow to Saint Petersburg?",
            onPromptChanged = {},
        )
    }
}

@Preview
@Composable
private fun ChatUiPromptFieldWorkingPreview() {
    ThoonPreview {
        ChatUiPromptField(
            initialPrompt = "",
            onPromptChanged = {},
            isWorking = true,
        )
    }
}

@Preview
@Composable
private fun ChatUiPromptFieldLoadingPreview() {
    ThoonPreview {
        ChatUiPromptField(
            initialPrompt = "",
            onPromptChanged = {},
            isLoading = true,
        )
    }
}
