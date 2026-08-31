package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agentconfig.presentation.mapper.describe
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList

/**
 * The saved connection profiles.
 *
 * Every row opens the editor, and setting the default or deleting happens there. The alternative —
 * tapping a row to make it the default, with edit somewhere else — needs a row with two tap targets,
 * which means a richer [SettingItem] than a settings list should need. A list where a tap might do
 * one of two things is also a list people stop trusting.
 */
internal class AgentConfigListPresenter(
    private val navigator: Navigator,
    private val observeConfigs: ObserveAgentConfigsUseCase,
    private val observeDefaultConfig: ObserveDefaultAgentConfigUseCase,
) : Presenter<SettingsUiState> {

    @Composable
    override fun present(): SettingsUiState {
        val configs by remember { observeConfigs() }.collectAsState(initial = emptyList())
        val default by remember { observeDefaultConfig() }.collectAsState(initial = null)

        return SettingsUiState(
            title = TITLE,
            items = items(configs, default?.id),
            onBack = navigator::pop,
        )
    }

    private fun items(configs: List<AgentConfig>, defaultId: AgentConfigId?) = buildList {
        if (configs.isEmpty()) {
            add(
                SettingItem.Placeholder(
                    "No configurations yet. Add one to tell the agent which model to use and " +
                        "where to reach it.",
                ),
            )
        }

        configs.forEach { config ->
            add(
                SettingItem.Navigation(
                    title = config.name,
                    // The default is called out here because it is the answer to "which model
                    // answers when a chat has not chosen one", and nothing else on screen says so.
                    subtitle = config.describe().let {
                        if (config.id == defaultId) "Default · $it" else it
                    },
                    onClick = { navigator.goTo(AgentConfigEditorScreen(config.id.value)) },
                ),
            )
        }

        add(
            SettingItem.Navigation(
                title = "Add a configuration",
                onClick = { navigator.goTo(AgentConfigEditorScreen()) },
            ),
        )
    }.toPersistentList()

    private companion object {
        const val TITLE = "Model & Provider"
    }
}
