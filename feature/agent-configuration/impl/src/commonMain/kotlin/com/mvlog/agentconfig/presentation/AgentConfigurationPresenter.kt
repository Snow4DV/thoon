package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Composable
import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.agentconfig.api.ConfigurationSection
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter

/**
 * The whole settings tree, from one presenter.
 *
 * The root is a section whose rows navigate, so it needs no special case, and adding a section
 * costs an entry in [ConfigurationSection] plus a branch in [configurationSectionItems] — no Screen,
 * Presenter, Ui, factory or registration.
 */
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

    /**
     * Model & Provider skips the generic section: it is CRUD, not a settings list, so the root row
     * leads straight to its own screen rather than to a page holding one link.
     */
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
