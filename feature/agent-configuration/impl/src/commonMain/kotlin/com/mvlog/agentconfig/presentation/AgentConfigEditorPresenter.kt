package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.mvlog.agent.api.engine.LocalEnginesCollector
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.errorOrNull
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase
import com.mvlog.agentconfig.presentation.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.mapper.ConfigField
import com.mvlog.agentconfig.presentation.mapper.ConfigKind
import com.mvlog.agentconfig.presentation.mapper.field
import com.mvlog.agentconfig.presentation.mapper.message
import com.mvlog.agentconfig.presentation.mapper.toDraft
import com.mvlog.agentconfig.presentation.mapper.toForm
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class AgentConfigEditorPresenter(
    private val screen: AgentConfigEditorScreen,
    private val navigator: Navigator,
    private val observeConfig: ObserveAgentConfigUseCase,
    private val observeDefaultConfig: ObserveDefaultAgentConfigUseCase,
    private val createConfig: CreateAgentConfigUseCase,
    private val updateConfig: UpdateAgentConfigUseCase,
    private val deleteConfig: DeleteAgentConfigUseCase,
    private val setDefaultConfig: SetDefaultAgentConfigUseCase,
) : Presenter<AgentConfigEditorUiState> {

    @Composable
    override fun present(): AgentConfigEditorUiState {
        val scope = rememberCoroutineScope()
        val configId = screen.configId?.let(::AgentConfigId)

        var form by remember { mutableStateOf(AgentConfigForm()) }
        var fieldError by remember { mutableStateOf<Pair<ConfigField, String>?>(null) }
        var screenError by remember { mutableStateOf<String?>(null) }
        var isDefault by remember { mutableStateOf(false) }

        // Loaded once rather than collected: this is a form, and letting a background write replace
        // what the user is halfway through typing would be worse than showing a stale value.
        LaunchedEffect(configId) {
            val existing = configId?.let { observeConfig(it).first() }
            if (existing != null) form = existing.toForm()
            isDefault = configId != null && observeDefaultConfig().first()?.id == configId
        }

        val engines = remember { LocalEnginesCollector.obtain().toPersistentList() }

        return AgentConfigEditorUiState(
            form = form,
            localEngines = engines,
            fieldError = fieldError,
            screenError = screenError,
            isEditing = configId != null,
            isDefault = isDefault,
            // An on-device configuration cannot be saved while no engine exists to select, which is
            // the empty registry made visible rather than a silently broken profile.
            canSave = form.kind != ConfigKind.Local || engines.isNotEmpty(),
            eventSink = { event ->
                when (event) {
                    is AgentConfigEditorEvent.FormChanged -> {
                        form = event.form
                        // Clearing on edit: leaving a rejection under a field the user has since
                        // corrected is how a form starts lying.
                        fieldError = null
                        screenError = null
                    }

                    AgentConfigEditorEvent.SaveClicked -> scope.launch {
                        val result = if (configId == null) {
                            createConfig(form.toDraft())
                        } else {
                            updateConfig(configId, form.toDraft())
                        }

                        val error = result.errorOrNull()
                        if (error == null) {
                            navigator.pop()
                        } else {
                            // Typed validation exists precisely so this can land under the right
                            // input instead of in a banner saying "invalid".
                            fieldError = error.field()?.let { it to error.message() }
                            screenError = if (error.field() == null) error.message() else null
                        }
                    }

                    AgentConfigEditorEvent.DeleteClicked -> scope.launch {
                        configId?.let { deleteConfig(it) }
                        navigator.pop()
                    }

                    AgentConfigEditorEvent.MakeDefaultClicked -> scope.launch {
                        configId?.let {
                            setDefaultConfig(it)
                            isDefault = true
                        }
                    }

                    AgentConfigEditorEvent.BackClicked -> navigator.pop()
                }
            },
        )
    }
}
