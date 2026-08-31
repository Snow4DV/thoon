package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Immutable
import com.mvlog.agent.api.engine.LocalEngine
import com.mvlog.agentconfig.presentation.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.mapper.ConfigField
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.PersistentList

/**
 * The editor's own state, deliberately not a list of [SettingItem].
 *
 * This is a form: which protocol is selected decides which inputs exist, a rejection belongs under
 * one specific input, and saving is a distinct act rather than a per-row side effect. Expressing
 * that through the settings renderer would mean giving [SettingItem] conditional field sets and
 * per-field errors — a form library inside a settings model.
 */
@Immutable
internal data class AgentConfigEditorUiState(
    val form: AgentConfigForm,
    /** Empty until some module ships an on-device engine; see [LocalEngine]. */
    val localEngines: PersistentList<LocalEngine>,
    val fieldError: Pair<ConfigField, String>?,
    val screenError: String?,
    val isEditing: Boolean,
    val isDefault: Boolean,
    val canSave: Boolean,
    val eventSink: (AgentConfigEditorEvent) -> Unit,
) : CircuitUiState

internal sealed interface AgentConfigEditorEvent : CircuitUiEvent {

    data class FormChanged(val form: AgentConfigForm) : AgentConfigEditorEvent

    data object SaveClicked : AgentConfigEditorEvent

    data object DeleteClicked : AgentConfigEditorEvent

    data object MakeDefaultClicked : AgentConfigEditorEvent

    data object BackClicked : AgentConfigEditorEvent
}
