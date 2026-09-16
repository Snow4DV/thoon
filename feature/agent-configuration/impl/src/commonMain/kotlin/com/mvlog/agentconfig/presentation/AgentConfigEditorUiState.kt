package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Immutable
import com.mvlog.agent.api.engine.LocalEngine
import com.mvlog.agentconfig.presentation.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.mapper.ConfigField
import com.mvlog.agentconfig.presentation.mapper.ConfigKind
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
 *
 * Sealed so that an existing configuration is never drawn as a blank form under an "Edit" title
 * while it loads — a keystroke made in that frame would be overwritten by the load landing. Each
 * variant carries only what can be done in it: going back is possible everywhere, so [onBack] is
 * on every variant; editing and saving are possible only with a form, so the event sink is on
 * [Data] alone.
 */
@Immutable
internal sealed interface AgentConfigEditorUiState : CircuitUiState {

    val onBack: () -> Unit

    /** Only an existing configuration loads, so this variant is always under an "Edit" title. */
    data class Loading(override val onBack: () -> Unit) : AgentConfigEditorUiState

    /** The configuration this screen was opened for is gone; there is nothing to edit. */
    data class Error(
        val message: String,
        override val onBack: () -> Unit,
    ) : AgentConfigEditorUiState

    data class Data(
        val form: AgentConfigForm,
        /** Empty until some module ships an on-device engine; see [LocalEngine]. */
        val localEngines: PersistentList<LocalEngine>,
        val rejection: Rejection?,
        /** Null while creating. Present only when editing a saved configuration. */
        val existing: Existing?,
        /** A save, delete or make-default call is in flight; the buttons that write are held. */
        val isBusy: Boolean,
        val canSave: Boolean,
        override val onBack: () -> Unit,
        val eventSink: (AgentConfigEditorUiEvent) -> Unit,
    ) : AgentConfigEditorUiState
}

/**
 * What is true of a saved configuration that is not true of a new one.
 *
 * Nested rather than flattened into `isEditing` and `isDefault` flags on [AgentConfigEditorUiState.Data]:
 * a default flag on a configuration that does not exist yet has no meaning, and a value that is
 * only meaningful when another flag is set is a state the type should not be able to express.
 */
internal data class Existing(val isDefault: Boolean)

/**
 * Why the last save was refused, and where to show it.
 *
 * One value rather than a field error and a screen error side by side: a refusal is about one input
 * or about none, never both, so two nullable slots would allow a state that cannot happen.
 */
internal sealed interface Rejection {

    /** Belongs under one input. */
    data class Field(val field: ConfigField, val message: String) : Rejection

    /** About no one input; shown as a banner. */
    data class Screen(val message: String) : Rejection
}

/**
 * What the user did to the form, not what the form should now contain.
 *
 * The UI reports intents and the presenter applies them, so the UI carries no knowledge of how a
 * protocol switch or a field edit changes the form — see `docs/circuit/PRESENTER.md`. Going back is
 * not here: it is possible in every variant, so it is `onBack` on the state instead.
 */
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
