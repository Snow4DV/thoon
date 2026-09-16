package com.mvlog.agentconfig.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.destructiveColor
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.agentconfig.presentation.AgentConfigEditorUiEvent.Ui
import com.mvlog.agentconfig.presentation.AgentConfigEditorUiState
import com.mvlog.agentconfig.presentation.Rejection
import com.mvlog.agentconfig.presentation.mapper.ConfigKind
import com.mvlog.agentconfig.presentation.mapper.read
import com.mvlog.agentconfig.presentation.mapper.title
import com.mvlog.agentconfig.presentation.ui.component.ConfigTextField
import com.mvlog.agentconfig.presentation.ui.component.OptionRow
import com.mvlog.ui.ThoonTypography
import com.mvlog.ui.components.ThoonTopBar

@Composable
internal fun AgentConfigEditorUi(
    state: AgentConfigEditorUiState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ThoonTopBar(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme[colors][secondaryColor])
                .statusBarsPadding(),
            title = { Text(state.title(), fontSize = 20.sp) },
            onBackClicked = state.onBack,
            onOptionsClick = null,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state) {
                // Nothing to draw yet: a blank form here would invite typing into fields the load
                // is about to overwrite.
                is AgentConfigEditorUiState.Loading -> Unit

                is AgentConfigEditorUiState.Error -> Text(
                    text = state.message,
                    style = ThoonTypography.caption,
                    color = Theme[colors][destructiveColor],
                )

                is AgentConfigEditorUiState.Data -> EditorForm(state)
            }
        }
    }
}

/** Only a new configuration is ever "new": loading and missing both concern a saved one. */
private fun AgentConfigEditorUiState.title(): String =
    if (this is AgentConfigEditorUiState.Data && existing == null) "New configuration" else "Edit configuration"

@Composable
private fun EditorForm(state: AgentConfigEditorUiState.Data) {
    Text("Protocol", style = ThoonTypography.h3)
    ConfigKind.entries.forEach { kind ->
        OptionRow(
            title = kind.title(),
            isSelected = state.form.kind == kind,
            onSelect = { state.eventSink(Ui.KindSelected(kind)) },
        )
    }

    // Only the fields this protocol actually uses. The others are not optional, they are
    // meaningless — an Ollama endpoint has no API key.
    state.form.visibleFields.forEach { field ->
        ConfigTextField(
            field = field,
            value = state.form.read(field),
            error = (state.rejection as? Rejection.Field)?.takeIf { it.field == field }?.message,
            onValueChange = { value -> state.eventSink(Ui.FieldChanged(field, value)) },
        )
    }

    if (state.form.kind == ConfigKind.Local) {
        if (state.localEngines.isEmpty()) {
            // Shown rather than hidden: the concept exists and is coming, and a silently
            // missing option reads as a bug.
            Text(
                text = "No on-device engines are available yet. Nothing in the app " +
                    "implements one, so this cannot be saved.",
                style = ThoonTypography.caption,
                color = Theme[colors][mutedColor],
            )
        } else {
            state.localEngines.forEach { engine ->
                OptionRow(
                    title = engine.displayName,
                    isSelected = state.form.engineId == engine.id,
                    onSelect = { state.eventSink(Ui.EngineSelected(engine.id)) },
                )
            }
        }
    }

    (state.rejection as? Rejection.Screen)?.let {
        Text(text = it.message, style = ThoonTypography.caption, color = Theme[colors][destructiveColor])
    }

    Button(
        onClick = { state.eventSink(Ui.SaveClicked) },
        style = ButtonStyle.Primary,
        enabled = state.canSave,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (state.isBusy) "Saving…" else "Save")
    }

    state.existing?.let { existing ->
        if (!existing.isDefault) {
            Button(
                onClick = { state.eventSink(Ui.MakeDefaultClicked) },
                style = ButtonStyle.Outlined,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use as default")
            }
        } else {
            Text(
                text = "This is the default configuration.",
                style = ThoonTypography.caption,
                color = Theme[colors][mutedColor],
            )
        }

        Button(
            onClick = { state.eventSink(Ui.DeleteClicked) },
            style = ButtonStyle.Ghost,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Delete", color = Theme[colors][destructiveColor])
        }
    }
}
