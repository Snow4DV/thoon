package com.mvlog.chatslist.presentation.ui.component

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

/** Debounced: every reported query is a database query. */
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

    // Auto-focus: choosing the tab is the intent.
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }

    TextField(
        state = state,
        modifier = modifier.focusRequester(focusRequester),
        placeholder = { Text("Search chats…", color = Theme[colors][mutedColor]) },
    )
}

private const val SEARCH_DEBOUNCE_MILLIS = 200L
