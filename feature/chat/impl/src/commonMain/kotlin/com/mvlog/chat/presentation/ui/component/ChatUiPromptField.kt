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
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text
import com.composables.ui.components.TextField
import com.composables.ui.theme.colors
import com.composables.ui.theme.controlColor
import com.composables.ui.theme.fieldColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.onFieldColor
import com.composeunstyled.theme.Theme
import com.mvlog.ui.ThoonPreview
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

/**
 * The chat prompt input.
 *
 * [TextFieldState] is owned locally (seeded once from [initialPrompt]) rather than driven by
 * [onPromptChanged]'s caller, so every keystroke renders immediately regardless of how the
 * caller handles the callback. If [onPromptChanged] routed straight through to an eventSink
 * consumed asynchronously by a Presenter, waiting on that round trip to reflect the next
 * keystroke would visibly lag or drop input under load. Instead, [onPromptChanged] is only
 * notified as a debounced side effect off the input path, so it's free to be slow.
 */
@OptIn(FlowPreview::class)
@Composable
fun ChatUiPromptField(
    initialPrompt: String,
    onPromptChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onSubmit: (String) -> Unit = {},
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

    // Derived so the button's enabled state recomposes when blankness flips, not on every keystroke.
    val canSend by remember(textFieldState) {
        derivedStateOf { textFieldState.text.isNotBlank() }
    }

    // Read straight from the field: the debounced [onPromptChanged] may not have fired yet, so
    // anything the caller holds can be up to 200ms stale. Shared by the button and the IME action
    // so the two cannot drift apart.
    fun submit() {
        val prompt = textFieldState.text.toString()
        if (prompt.isNotBlank()) {
            latestOnSubmit(prompt)
            textFieldState.clearText()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth().let { if (isLoading) it.shimmer() else it },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        // Beside the field rather than in its `trailing` slot: that slot aligns to the top for a
        // multi-line field, which would strand the button above a prompt as it grows.
        verticalAlignment = Alignment.Bottom,
    ) {
        TextField(
            state = textFieldState,
            modifier = Modifier.weight(1f),
            enabled = !isLoading,
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
            placeholder = { Text("Ask something…", color = Theme[colors][mutedColor]) },
            backgroundColor = if (isLoading) Theme[colors][controlColor] else Theme[colors][fieldColor],
            contentColor = if (isLoading) Theme[colors][mutedColor] else Theme[colors][onFieldColor],
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            onKeyboardAction = KeyboardActionHandler { submit() },
        )

        IconButton(
            onClick = ::submit,
            enabled = !isLoading && canSend,
            style = ButtonStyle.Primary,
        ) {
            Icon(Lucide.Send, contentDescription = "Send")
        }
    }
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
private fun ChatUiPromptFieldLoadingPreview() {
    ThoonPreview {
        ChatUiPromptField(
            initialPrompt = "",
            onPromptChanged = {},
            isLoading = true,
        )
    }
}
