package com.mvlog.agentconfig.presentation

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
import com.mvlog.agentconfig.presentation.AgentConfigEditorUiEvent.Ui
import com.mvlog.agentconfig.presentation.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.mapper.message
import com.mvlog.agentconfig.presentation.mapper.toDraft
import com.mvlog.agentconfig.presentation.mapper.write
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Creates or edits one connection profile.
 *
 * Holds exactly one thing, an [AgentConfigEditorStateHolder], and asks it for transitions; see
 * [AgentConfigEditorState] for why the state is one value and the holder for why it is wrapped. Whether the configuration is the default is observed, not held: setting it here
 * changes storage, and storage is what this screen reads back, so the flag cannot drift from the
 * truth.
 *
 * The draft is *fetched* once, not observed: this is a form, and letting a background write
 * replace what the user is halfway through typing would be worse than showing a stale value. That
 * is why the dependency is [GetAgentConfigUseCase] and not a flow.
 */
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

    /**
     * Every event, in one non-composable function.
     *
     * Kept out of `present()` so that function reads as observe, hold, return — and so the handling
     * can be read without Compose in the way. It takes the holder and the scope as parameters rather
     * than closing over composable locals, which is what keeps it a plain function.
     */
    private fun handleEvent(
        event: AgentConfigEditorUiEvent,
        stateHolder: AgentConfigEditorStateHolder,
        scope: CoroutineScope,
    ) {
        when (event) {
            is Ui.KindSelected -> stateHolder.edit { copy(kind = event.kind) }
            is Ui.FieldChanged -> stateHolder.edit { write(event.field, event.value) }
            is Ui.EngineSelected -> stateHolder.edit { copy(engineId = event.engineId) }

            Ui.SaveClicked -> stateHolder.saveableForm(localEngines)?.let { form ->
                scope.launch {
                    stateHolder.busy()
                    val error = save(screen.configId, form)
                    if (error == null) navigator.pop() else stateHolder.rejected(error)
                }
            }

            Ui.DeleteClicked -> screen.configId?.let { id ->
                if (stateHolder.idleForm() != null) scope.launch {
                    stateHolder.busy()
                    deleteConfig(id)
                    navigator.pop()
                }
            }

            Ui.MakeDefaultClicked -> screen.configId?.let { id ->
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
