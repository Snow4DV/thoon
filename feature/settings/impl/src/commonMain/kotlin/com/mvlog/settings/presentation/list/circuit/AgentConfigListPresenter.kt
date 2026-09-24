package com.mvlog.settings.presentation.list.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.settings.api.SettingsSection
import com.mvlog.settings.presentation.common.settings.describe
import com.mvlog.settings.presentation.common.settings.ui.SettingItem
import com.mvlog.settings.presentation.common.settings.ui.SettingsUiState
import com.mvlog.settings.presentation.settings.circuit.title
import com.mvlog.settings.presentation.editor.AgentConfigEditorScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.collections.immutable.toPersistentList

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
            title = SettingsSection.ModelAndProvider.title(),
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
                    subtitle = config.describe().let {
                        if (config.id == defaultId) "Default · $it" else it
                    },
                    onClick = { navigator.goTo(AgentConfigEditorScreen(config.id)) },
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
}
