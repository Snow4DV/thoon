package com.mvlog.agentconfig.presentation.configuration.circuit

import androidx.compose.runtime.Composable
import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.agentconfig.api.ConfigurationSection
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingsUiState
import com.mvlog.agentconfig.presentation.list.AgentConfigListScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter

internal class AgentConfigurationPresenter(
    private val screen: AgentConfigurationScreen,
    private val navigator: Navigator,
) : Presenter<SettingsUiState> {

    @Composable
    override fun present(): SettingsUiState = SettingsUiState(
        title = screen.section?.title() ?: ROOT_TITLE,
        items = when (val section = screen.section) {
            null -> configurationRootItems(onOpen = ::open)
            else -> configurationSectionItems(section, onOpenModelAndProvider = ::openConfigurations)
        },
        onBack = navigator::pop,
    )

    private fun open(section: ConfigurationSection) = when (section) {
        ConfigurationSection.ModelAndProvider -> openConfigurations()
        else -> navigator.goTo(AgentConfigurationScreen(section))
    }

    private fun openConfigurations() {
        navigator.goTo(AgentConfigListScreen)
    }

    private companion object {
        const val ROOT_TITLE = "Settings"
    }
}
