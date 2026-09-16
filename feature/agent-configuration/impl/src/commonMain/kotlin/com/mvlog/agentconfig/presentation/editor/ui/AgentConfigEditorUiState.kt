package com.mvlog.agentconfig.presentation.editor.ui

import androidx.compose.runtime.Immutable
import com.mvlog.agent.api.engine.LocalEngine
import com.mvlog.agentconfig.presentation.editor.ui.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.editor.ui.mapper.ConfigField
import com.mvlog.agentconfig.presentation.editor.ui.mapper.ConfigKind
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.PersistentList

@Immutable
internal sealed interface AgentConfigEditorUiState : CircuitUiState {

    val onBack: () -> Unit

    data class Loading(override val onBack: () -> Unit) : AgentConfigEditorUiState

    data class Error(
        val message: String,
        override val onBack: () -> Unit,
    ) : AgentConfigEditorUiState

    data class Data(
        val form: AgentConfigForm,
        /** Empty until a module ships an on-device engine. */
        val localEngines: PersistentList<LocalEngine>,
        val rejection: Rejection?,
        val existing: Existing?,
        val isBusy: Boolean,
        val canSave: Boolean,
        override val onBack: () -> Unit,
        val eventSink: (AgentConfigEditorUiEvent) -> Unit,
    ) : AgentConfigEditorUiState
}

internal data class Existing(val isDefault: Boolean)

internal sealed interface Rejection {

    data class Field(val field: ConfigField, val message: String) : Rejection

    data class Screen(val message: String) : Rejection
}

/** No back event: going back is possible in every variant, so it is `onBack` on the state. */
internal sealed interface AgentConfigEditorUiEvent : CircuitUiEvent {

    sealed interface Ui : AgentConfigEditorUiEvent {

        data class KindSelected(val kind: ConfigKind) : Ui

        data class FieldChanged(val field: ConfigField, val value: String) : Ui

        data class EngineSelected(val engineId: String) : Ui

        data object SaveClicked : Ui

        data object DeleteClicked : Ui

        data object MakeDefaultClicked : Ui
    }
}
