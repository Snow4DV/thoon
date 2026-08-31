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
import androidx.compose.ui.text.input.KeyboardType
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
import com.mvlog.agentconfig.presentation.AgentConfigEditorEvent
import com.mvlog.agentconfig.presentation.AgentConfigEditorUiState
import com.mvlog.agentconfig.presentation.mapper.ConfigField
import com.mvlog.agentconfig.presentation.mapper.ConfigKind
import com.mvlog.agentconfig.presentation.mapper.read
import com.mvlog.agentconfig.presentation.mapper.write
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
            title = { Text(if (state.isEditing) "Edit configuration" else "New configuration", fontSize = 20.sp) },
            onBackClicked = { state.eventSink(AgentConfigEditorEvent.BackClicked) },
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
            Text("Protocol", style = ThoonTypography.h3)
            ConfigKind.entries.forEach { kind ->
                OptionRow(
                    title = kind.title(),
                    isSelected = state.form.kind == kind,
                    onSelect = {
                        state.eventSink(AgentConfigEditorEvent.FormChanged(state.form.copy(kind = kind)))
                    },
                )
            }

            // Only the fields this protocol actually uses. The others are not optional, they are
            // meaningless — an Ollama endpoint has no API key.
            state.form.visibleFields.forEach { field ->
                ConfigTextField(
                    field = field,
                    value = state.form.read(field),
                    error = state.fieldError?.takeIf { it.first == field }?.second,
                    // The form is read here, in a lambda rebuilt every composition, so an edit
                    // always applies to the protocol currently selected rather than to whichever
                    // one was selected when the field first appeared.
                    onValueChange = { value ->
                        state.eventSink(
                            AgentConfigEditorEvent.FormChanged(state.form.write(field, value)),
                        )
                    },
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
                            onSelect = {
                                state.eventSink(
                                    AgentConfigEditorEvent.FormChanged(state.form.copy(engineId = engine.id)),
                                )
                            },
                        )
                    }
                }
            }

            state.screenError?.let {
                Text(text = it, style = ThoonTypography.caption, color = Theme[colors][destructiveColor])
            }

            Button(
                onClick = { state.eventSink(AgentConfigEditorEvent.SaveClicked) },
                style = ButtonStyle.Primary,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }

            if (state.isEditing) {
                if (!state.isDefault) {
                    Button(
                        onClick = { state.eventSink(AgentConfigEditorEvent.MakeDefaultClicked) },
                        style = ButtonStyle.Outlined,
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
                    onClick = { state.eventSink(AgentConfigEditorEvent.DeleteClicked) },
                    style = ButtonStyle.Ghost,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete", color = Theme[colors][destructiveColor])
                }
            }
        }
    }
}
