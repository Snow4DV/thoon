package com.mvlog.settings.presentation.settings.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mvlog.agent.api.usecase.AddToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.GetApprovalGatedToolsUseCase
import com.mvlog.agent.api.usecase.ObserveToolApprovalRulesUseCase
import com.mvlog.agent.api.usecase.RevokeToolApprovalRuleUseCase
import com.mvlog.settings.api.SettingsScreen
import com.mvlog.settings.api.SettingsSection
import com.mvlog.settings.presentation.common.settings.ui.SettingsUiState
import com.mvlog.settings.presentation.list.AgentConfigListScreen
import com.mvlog.usersettings.api.usecase.ObserveThemeModeUseCase
import com.mvlog.usersettings.api.usecase.SetThemeModeUseCase
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch

internal class SettingsPresenter(
    private val screen: SettingsScreen,
    private val navigator: Navigator,
    private val observeToolApprovalRules: ObserveToolApprovalRulesUseCase,
    private val addToolApprovalRule: AddToolApprovalRuleUseCase,
    private val revokeToolApprovalRule: RevokeToolApprovalRuleUseCase,
    private val getApprovalGatedTools: GetApprovalGatedToolsUseCase,
    private val observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : Presenter<SettingsUiState> {

    @Composable
    override fun present(): SettingsUiState {
        val scope = rememberCoroutineScope()
        val globalRules by remember { observeToolApprovalRules(null) }.collectAsState(initial = emptyList())
        val gatedTools = remember { getApprovalGatedTools() }
        val themeMode by remember { observeThemeMode() }.collectAsState()

        return SettingsUiState(
            title = screen.section?.title() ?: ROOT_TITLE,
            items = when (val section = screen.section) {
                null -> settingsRootItems(onOpen = ::open)
                else -> settingsSectionItems(
                    section = section,
                    onOpenModelAndProvider = ::openConfigurations,
                    gatedTools = gatedTools,
                    approvalRules = globalRules,
                    onAllowTool = { tool -> scope.launch { addToolApprovalRule(tool, null) } },
                    onRevokeRule = { id -> scope.launch { revokeToolApprovalRule(id) } },
                    themeMode = themeMode,
                    onSelectThemeMode = { mode -> setThemeMode(mode) },
                )
            },
            onBack = navigator::pop,
        )
    }

    private fun open(section: SettingsSection) = when (section) {
        SettingsSection.ModelAndProvider -> openConfigurations()
        else -> navigator.goTo(SettingsScreen(section))
    }

    private fun openConfigurations() {
        navigator.goTo(AgentConfigListScreen)
    }

    private companion object {
        const val ROOT_TITLE = "Settings"
    }
}
