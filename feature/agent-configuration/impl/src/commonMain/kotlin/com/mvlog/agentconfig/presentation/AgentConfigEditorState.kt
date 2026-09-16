package com.mvlog.agentconfig.presentation

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agentconfig.presentation.mapper.AgentConfigForm

/**
 * Everything the editor presenter owns, as one value.
 *
 * Distinct from [AgentConfigEditorUiState], which is what the UI renders: that one also carries what
 * the presenter *observes* — the default configuration, the engine registry — and the event sink.
 * This one is only what the presenter *holds*, so it is what gets retained across rotation.
 *
 * One value rather than a handful of `remember`s so that a transition is a single snapshot write.
 * An edit and the clearing of its rejection, or a save settling and its error landing, are one
 * state change each — a frame showing the new text under the old rejection is not a state this
 * screen can be in.
 *
 * Plain data: it has no behaviour of its own. Every transition lives in
 * [AgentConfigEditorStateHolder], which is the only thing that produces a new one.
 */
internal sealed interface AgentConfigEditorState {

    /** The saved configuration has not arrived yet; the UI must not offer a form. */
    data object Loading : AgentConfigEditorState

    /** The screen was opened for a configuration that no longer exists. */
    data class Missing(val id: AgentConfigId) : AgentConfigEditorState

    data class Editing(
        val form: AgentConfigForm,
        val rejection: Rejection? = null,
        /** A save, delete or make-default call is in flight; writes are held until it settles. */
        val isBusy: Boolean = false,
    ) : AgentConfigEditorState
}
