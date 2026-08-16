package com.mvlog.chat.presentation.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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

    TextField(
        state = textFieldState,
        modifier = modifier.fillMaxWidth().let { if (isLoading) it.shimmer() else it },
        enabled = !isLoading,
        lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = 5),
        placeholder = { Text("Ask something…", color = Theme[colors][mutedColor]) },
        backgroundColor = if (isLoading) Theme[colors][controlColor] else Theme[colors][fieldColor],
        contentColor = if (isLoading) Theme[colors][mutedColor] else Theme[colors][onFieldColor],
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        onKeyboardAction = KeyboardActionHandler {
            // Read straight from the field: the debounced [onPromptChanged] may not have fired yet,
            // so anything the caller holds can be up to 200ms stale.
            val prompt = textFieldState.text.toString()
            if (prompt.isNotBlank()) {
                latestOnSubmit(prompt)
                textFieldState.clearText()
            }
        },
    )
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
private fun ChatUiPromptFieldLoadingPreview() {
    ThoonPreview {
        ChatUiPromptField(
            initialPrompt = "",
            onPromptChanged = {},
            isLoading = true,
        )
    }
}
