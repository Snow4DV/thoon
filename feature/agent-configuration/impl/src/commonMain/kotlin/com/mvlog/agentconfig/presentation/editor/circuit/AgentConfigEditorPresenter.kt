package com.mvlog.agentconfig.presentation.editor.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mvlog.agent.api.engine.LocalEngine
import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.errorOrNull
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase
import com.mvlog.agentconfig.presentation.editor.AgentConfigEditorScreen
import com.mvlog.agentconfig.presentation.editor.ui.AgentConfigEditorUiEvent
import com.mvlog.agentconfig.presentation.editor.ui.AgentConfigEditorUiState
import com.mvlog.agentconfig.presentation.editor.ui.Existing
import com.mvlog.agentconfig.presentation.editor.ui.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.editor.ui.mapper.message
import com.mvlog.agentconfig.presentation.editor.ui.mapper.toDraft
import com.mvlog.agentconfig.presentation.editor.ui.mapper.write
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** The draft is fetched once, not observed: after load it is the truth, not storage. */
internal class AgentConfigEditorPresenter(
    private val screen: AgentConfigEditorScreen,
    private val navigator: Navigator,
    private val localEngines: List<LocalEngine>,
    private val getConfig: GetAgentConfigUseCase,
    private val observeDefaultConfig: ObserveDefaultAgentConfigUseCase,
    private val createConfig: CreateAgentConfigUseCase,
    private val updateConfig: UpdateAgentConfigUseCase,
    private val deleteConfig: DeleteAgentConfigUseCase,
    private val setDefaultConfig: SetDefaultAgentConfigUseCase,
) : Presenter<AgentConfigEditorUiState> {

    @Composable
    override fun present(): AgentConfigEditorUiState {
        val scope = rememberCoroutineScope()
        val editor = rememberRetained { AgentConfigEditorStateHolder(screen.configId) }

        LaunchedEffect(screen.configId) {
            screen.configId?.let { editor.loaded(it, getConfig(it)) }
        }

        val default by remember { observeDefaultConfig() }.collectAsState(initial = null)
        val engines = remember(localEngines) { localEngines.toPersistentList() }

        return when (val current = editor.value) {
            AgentConfigEditorState.Loading -> AgentConfigEditorUiState.Loading(onBack = navigator::pop)

            is AgentConfigEditorState.Missing -> AgentConfigEditorUiState.Error(
                message = AgentConfigError.NotFound(current.id).message(),
                onBack = navigator::pop,
            )

            is AgentConfigEditorState.Editing -> AgentConfigEditorUiState.Data(
                form = current.form,
                localEngines = engines,
                rejection = current.rejection,
                existing = screen.configId?.let { Existing(isDefault = default?.id == it) },
                isBusy = current.isBusy,
                canSave = editor.canSave(localEngines),
                onBack = navigator::pop,
                eventSink = { handleEvent(it, editor, scope) },
            )
        }
    }

    private fun handleEvent(
        event: AgentConfigEditorUiEvent,
        stateHolder: AgentConfigEditorStateHolder,
        scope: CoroutineScope,
    ) {
        when (event) {
            is AgentConfigEditorUiEvent.Ui.KindSelected -> stateHolder.edit { copy(kind = event.kind) }
            is AgentConfigEditorUiEvent.Ui.FieldChanged -> stateHolder.edit { write(event.field, event.value) }
            is AgentConfigEditorUiEvent.Ui.EngineSelected -> stateHolder.edit { copy(engineId = event.engineId) }

            AgentConfigEditorUiEvent.Ui.SaveClicked -> stateHolder.saveableForm(localEngines)?.let { form ->
                scope.launch {
                    stateHolder.busy()
                    val error = save(screen.configId, form)
                    if (error == null) navigator.pop() else stateHolder.rejected(error)
                }
            }

            AgentConfigEditorUiEvent.Ui.DeleteClicked -> screen.configId?.let { id ->
                if (stateHolder.idleForm() != null) scope.launch {
                    stateHolder.busy()
                    deleteConfig(id)
                    navigator.pop()
                }
            }

            AgentConfigEditorUiEvent.Ui.MakeDefaultClicked -> screen.configId?.let { id ->
                if (stateHolder.idleForm() != null) scope.launch {
                    stateHolder.busy()
                    setDefaultConfig(id)
                    stateHolder.settled()
                }
            }
        }
    }

    private suspend fun save(configId: AgentConfigId?, form: AgentConfigForm): AgentConfigError? {
        val result = if (configId == null) {
            createConfig(form.toDraft())
        } else {
            updateConfig(configId, form.toDraft())
        }
        return result.errorOrNull()
    }
}