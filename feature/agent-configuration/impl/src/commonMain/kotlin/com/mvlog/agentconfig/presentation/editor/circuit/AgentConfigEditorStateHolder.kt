package com.mvlog.agentconfig.presentation.editor.circuit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mvlog.agent.api.engine.LocalEngine
import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agentconfig.presentation.editor.circuit.AgentConfigEditorState.Editing
import com.mvlog.agentconfig.presentation.editor.circuit.AgentConfigEditorState.Loading
import com.mvlog.agentconfig.presentation.editor.circuit.AgentConfigEditorState.Missing
import com.mvlog.agentconfig.presentation.editor.ui.Rejection
import com.mvlog.agentconfig.presentation.editor.ui.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.editor.ui.mapper.ConfigKind
import com.mvlog.agentconfig.presentation.editor.ui.mapper.field
import com.mvlog.agentconfig.presentation.editor.ui.mapper.message
import com.mvlog.agentconfig.presentation.editor.ui.mapper.toForm

/** Every transition is a no-op on a variant it does not apply to; callers never check first. */
@Stable
internal class AgentConfigEditorStateHolder(configId: AgentConfigId?) {

    var value: AgentConfigEditorState by mutableStateOf(
        if (configId == null) AgentConfigEditorState.Editing(AgentConfigForm()) else AgentConfigEditorState.Loading,
    )
        private set

    private val editing: AgentConfigEditorState.Editing?
        get() = value as? AgentConfigEditorState.Editing

    /** Only while still loading: once a draft exists it is the truth, so a re-fetch is harmless. */
    fun loaded(id: AgentConfigId, existing: AgentConfig?) {
        if (value !is AgentConfigEditorState.Loading) return
        value = existing?.let { AgentConfigEditorState.Editing(it.toForm()) } ?: AgentConfigEditorState.Missing(
            id
        )
    }

    /** Clears the rejection: it was about a form the user has since changed. */
    fun edit(change: AgentConfigForm.() -> AgentConfigForm) {
        val current = editing ?: return
        value = current.copy(form = current.form.change(), rejection = null)
    }

    fun busy() {
        value = editing?.copy(isBusy = true) ?: return
    }

    fun settled() {
        value = editing?.copy(isBusy = false) ?: return
    }

    /** Applied to the form as it is now, so anything typed while the save was in flight is kept. */
    fun rejected(error: AgentConfigError) {
        val current = editing ?: return
        val rejection = error.field()
            ?.let { Rejection.Field(it, error.message()) }
            ?: Rejection.Screen(error.message())
        value = current.copy(rejection = rejection, isBusy = false)
    }

    fun idleForm(): AgentConfigForm? = editing?.takeUnless { it.isBusy }?.form

    /** An on-device configuration needs an engine to select; an empty registry blocks the save. */
    fun canSave(engines: List<LocalEngine>): Boolean {
        val current = editing ?: return false
        return !current.isBusy && (current.form.kind != ConfigKind.Local || engines.isNotEmpty())
    }

    fun saveableForm(engines: List<LocalEngine>): AgentConfigForm? =
        if (canSave(engines)) editing?.form else null
}