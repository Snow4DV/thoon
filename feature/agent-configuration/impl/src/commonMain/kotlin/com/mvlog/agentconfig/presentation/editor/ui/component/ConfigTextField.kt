package com.mvlog.agentconfig.presentation.editor.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.ui.components.Icon
import com.composables.ui.components.Text
import com.composables.ui.components.TextField
import com.composables.ui.theme.colors
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.agentconfig.presentation.editor.ui.mapper.ConfigField
import com.mvlog.ui.ThoonTypography
import kotlinx.coroutines.flow.drop

/**
 * Takes a value and reports a value, never the form: a form-passing version closed over a stale
 * form in the effect below and reset the protocol on the next keystroke.
 */
@Composable
internal fun ConfigTextField(
    field: ConfigField,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: TextFieldState = rememberTextFieldState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .drop(1) // the initial value, echoed back
            .collect { latestOnValueChange(it) }
    }

    // `rememberTextFieldState` reads its argument once, and an existing configuration loads after
    // that. Guarded on inequality so a keystroke never fights the value it just produced.
    LaunchedEffect(value) {
        if (state.text.toString() != value) state.setTextAndPlaceCursorAtEnd(value)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = field.label(), style = ThoonTypography.caption, color = Theme[colors][mutedColor])
        TextField(state = state, modifier = Modifier.fillMaxWidth())
        error?.let {
            Text(text = it, style = ThoonTypography.caption, color = Theme[colors][destructiveColor])
        }
    }
}

@Composable
internal fun OptionRow(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = title, style = ThoonTypography.body, modifier = Modifier.weight(1f))
        if (isSelected) {
            Icon(
                imageVector = Lucide.Check,
                tint = Theme[colors][primaryColor],
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun ConfigField.label(): String = when (this) {
    ConfigField.Name -> "Name"
    ConfigField.ModelId -> "Model id"
    ConfigField.ApiKey -> "API key"
    ConfigField.BaseUrl -> "Address"
    ConfigField.Engine -> "Engine"
}
