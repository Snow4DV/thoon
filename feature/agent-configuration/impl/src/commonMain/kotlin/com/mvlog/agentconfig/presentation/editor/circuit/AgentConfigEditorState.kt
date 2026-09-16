package com.mvlog.agentconfig.presentation.editor.circuit

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agentconfig.presentation.editor.ui.AgentConfigEditorUiState
import com.mvlog.agentconfig.presentation.editor.ui.Rejection
import com.mvlog.agentconfig.presentation.editor.ui.mapper.AgentConfigForm

internal sealed interface AgentConfigEditorState {

    /** The UI must not offer a form yet: a keystroke now would be overwritten by the load. */
    data object Loading : AgentConfigEditorState

    data class Missing(val id: AgentConfigId) : AgentConfigEditorState

    data class Editing(
        val form: AgentConfigForm,
        val rejection: Rejection? = null,
        /** Set by save, delete and make-default; every write is held while it is true. */
        val isBusy: Boolean = false,
    ) : AgentConfigEditorState
}