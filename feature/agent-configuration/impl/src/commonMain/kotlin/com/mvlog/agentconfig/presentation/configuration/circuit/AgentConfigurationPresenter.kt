package com.mvlog.agentconfig.presentation.configuration.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mvlog.agent.api.usecase.AddToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.GetApprovalGatedToolsUseCase
import com.mvlog.agent.api.usecase.ObserveToolApprovalRulesUseCase
import com.mvlog.agent.api.usecase.RevokeToolApprovalRuleUseCase
import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.agentconfig.api.ConfigurationSection
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingsUiState
import com.mvlog.agentconfig.presentation.list.AgentConfigListScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch

internal class AgentConfigurationPresenter(
    private val screen: AgentConfigurationScreen,
    private val navigator: Navigator,
    private val observeToolApprovalRules: ObserveToolApprovalRulesUseCase,
    private val addToolApprovalRule: AddToolApprovalRuleUseCase,
    private val revokeToolApprovalRule: RevokeToolApprovalRuleUseCase,
    private val getApprovalGatedTools: GetApprovalGatedToolsUseCase,
) : Presenter<SettingsUiState> {

    @Composable
    override fun present(): SettingsUiState {
        val scope = rememberCoroutineScope()
        val globalRules by remember { observeToolApprovalRules(null) }.collectAsState(initial = emptyList())
        val gatedTools = remember { getApprovalGatedTools() }

        return SettingsUiState(
            title = screen.section?.title() ?: ROOT_TITLE,
            items = when (val section = screen.section) {
                null -> configurationRootItems(onOpen = ::open)
                else -> configurationSectionItems(
                    section = section,
                    onOpenModelAndProvider = ::openConfigurations,
                    gatedTools = gatedTools,
                    approvalRules = globalRules,
                    onAllowTool = { tool -> scope.launch { addToolApprovalRule(tool, null) } },
                    onRevokeRule = { id -> scope.launch { revokeToolApprovalRule(id) } },
                )
            },
            onBack = navigator::pop,
        )
    }

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
