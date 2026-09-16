package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mvlog.agent.api.engine.LocalEngine
import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agentconfig.presentation.AgentConfigEditorState.Editing
import com.mvlog.agentconfig.presentation.AgentConfigEditorState.Loading
import com.mvlog.agentconfig.presentation.AgentConfigEditorState.Missing
import com.mvlog.agentconfig.presentation.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.mapper.ConfigKind
import com.mvlog.agentconfig.presentation.mapper.field
import com.mvlog.agentconfig.presentation.mapper.message
import com.mvlog.agentconfig.presentation.mapper.toForm

/**
 * The editor's retained state and the only way to change it.
 *
 * Circuit's form recipe keeps form state in a small `@Stable` holder created with `rememberRetained`
 * rather than in variables local to `present()`. The presenter's event branches call named
 * operations — [edit], [busy], [rejected] — so the set of things that can happen to the state is
 * this class's method list and nothing else. [AgentConfigEditorState] itself is plain data.
 *
 * Every transition is a no-op on a variant it does not apply to: a load landing on a draft, an edit
 * while still loading, a rejection with no form. That is what lets the presenter call them without
 * checking the variant first.
 *
 * Compose observes [value], so a transition here recomposes whatever read it. `mutableStateOf`
 * works outside a composition too, which is what makes this class testable with plain `kotlin.test`.
 */
@Stable
internal class AgentConfigEditorStateHolder(configId: AgentConfigId?) {

    /** A new configuration has nothing to load and starts editable. */
    var value: AgentConfigEditorState by mutableStateOf(
        if (configId == null) Editing(AgentConfigForm()) else Loading,
    )
        private set

    private val editing: Editing?
        get() = value as? Editing

    /**
     * The saved configuration arrived, or was found not to exist.
     *
     * Only a screen still loading takes it: once there is a draft, the draft is the truth and
     * storage is not. This is what lets the presenter re-fetch freely after rotation without
     * overwriting edits.
     */
    fun loaded(id: AgentConfigId, existing: AgentConfig?) {
        if (value !is Loading) return
        value = existing?.let { Editing(it.toForm()) } ?: Missing(id)
    }

    /**
     * The form changed. Clearing on edit: leaving a rejection under a field the user has since
     * corrected is how a form starts lying.
     */
    fun edit(change: AgentConfigForm.() -> AgentConfigForm) {
        val current = editing ?: return
        value = current.copy(form = current.form.change(), rejection = null)
    }

    /** A write started; every further write is held until [settled] or [rejected]. */
    fun busy() {
        value = editing?.copy(isBusy = true) ?: return
    }

    /** The write finished without complaint and the screen stays open. */
    fun settled() {
        value = editing?.copy(isBusy = false) ?: return
    }

    /**
     * The write was refused. Typed validation exists precisely so this can land under the right
     * input instead of in a banner saying "invalid"; a failure about no one input is the banner.
     *
     * Applied to the form as it is *now*, not as it was submitted: anything typed while the save
     * was in flight is kept.
     */
    fun rejected(error: AgentConfigError) {
        val current = editing ?: return
        val rejection = error.field()
            ?.let { Rejection.Field(it, error.message()) }
            ?: Rejection.Screen(error.message())
        value = current.copy(rejection = rejection, isBusy = false)
    }

    /** The form, if nothing is in flight — the precondition for delete and make-default. */
    fun idleForm(): AgentConfigForm? = editing?.takeUnless { it.isBusy }?.form

    /**
     * Whether Save may be offered. An on-device configuration cannot be saved while no engine
     * exists to select, which is the empty registry made visible rather than a silently broken
     * profile.
     */
    fun canSave(engines: List<LocalEngine>): Boolean {
        val current = editing ?: return false
        return !current.isBusy && (current.form.kind != ConfigKind.Local || engines.isNotEmpty())
    }

    /** The form, if [canSave] holds — the precondition for save. */
    fun saveableForm(engines: List<LocalEngine>): AgentConfigForm? =
        if (canSave(engines)) editing?.form else null
}
