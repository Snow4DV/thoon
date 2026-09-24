package com.mvlog.settings.presentation.chat.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mvlog.agent.api.usecase.AddToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.GetApprovalGatedToolsUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigOverrideUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveToolApprovalRulesUseCase
import com.mvlog.agent.api.usecase.RevokeToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.settings.api.ChatConfigurationScreen
import com.mvlog.settings.presentation.common.settings.ui.SettingsUiState
import com.mvlog.settings.presentation.list.AgentConfigListScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch

internal class ChatConfigurationPresenter(
    private val screen: ChatConfigurationScreen,
    private val navigator: Navigator,
    private val observeConfigs: ObserveAgentConfigsUseCase,
    private val observeChatConfigOverride: ObserveChatConfigOverrideUseCase,
    private val observeDefaultConfig: ObserveDefaultAgentConfigUseCase,
    private val setChatConfig: SetChatConfigUseCase,
    private val observeToolApprovalRules: ObserveToolApprovalRulesUseCase,
    private val addToolApprovalRule: AddToolApprovalRuleUseCase,
    private val revokeToolApprovalRule: RevokeToolApprovalRuleUseCase,
    private val getApprovalGatedTools: GetApprovalGatedToolsUseCase,
) : Presenter<SettingsUiState> {

    @Composable
    override fun present(): SettingsUiState {
        val scope = rememberCoroutineScope()

        val configs by remember { observeConfigs() }.collectAsState(initial = emptyList())
        val overrideId by remember(screen.chatId) { observeChatConfigOverride(screen.chatId) }.collectAsState(initial = null)
        val default by remember { observeDefaultConfig() }.collectAsState(initial = null)
        val rules by remember(screen.chatId) { observeToolApprovalRules(screen.chatId) }
            .collectAsState(initial = emptyList())
        val gatedTools = remember { getApprovalGatedTools() }

        return SettingsUiState(
            title = TITLE,
            items = chatConfigurationItems(
                configs = configs,
                overrideId = overrideId?.value,
                defaultName = default?.name,
                onSelect = { configId -> scope.launch { setChatConfig(screen.chatId, configId) } },
                onManageModels = { navigator.goTo(AgentConfigListScreen) },
                gatedTools = gatedTools,
                approvalRules = rules,
                onAllowTool = { tool -> scope.launch { addToolApprovalRule(tool, screen.chatId) } },
                onRevokeRule = { id -> scope.launch { revokeToolApprovalRule(id) } },
            ),
            onBack = navigator::pop,
        )
    }

    private companion object {
        const val TITLE = "Chat settings"
    }
}
