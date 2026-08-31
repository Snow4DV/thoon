package com.mvlog.chatslist.presentation.ui.component

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import com.composables.ui.components.Text
import com.composables.ui.components.TextField
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composeunstyled.theme.Theme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

/**
 * The search input.
 *
 * Owns its own [TextFieldState] and reports upward debounced, the same arrangement
 * `ChatUiPromptField` uses and for the same reason: driving the field from the caller makes every
 * keystroke a round trip through the presenter. Here it also matters downstream — each reported
 * query starts a new database query, so a debounce is the difference between one search and one per
 * character.
 */
@OptIn(FlowPreview::class)
@Composable
internal fun ChatsSearchField(
    initialQuery: String,
    onQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberTextFieldState(initialQuery)
    val latestOnQueryChanged by rememberUpdatedState(onQueryChanged)
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .drop(1) // the first emission is initialQuery echoed back; nothing changed yet
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .collectLatest { latestOnQueryChanged(it) }
    }

    // Selecting the tab is the whole intent; making the user then tap the field would be asking
    // twice.
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }

    TextField(
        state = state,
        modifier = modifier.focusRequester(focusRequester),
        placeholder = { Text("Search chats…", color = Theme[colors][mutedColor]) },
    )
}

private const val SEARCH_DEBOUNCE_MILLIS = 200L
