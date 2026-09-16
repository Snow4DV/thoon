package com.mvlog.agentconfig.presentation.chat.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agentconfig.api.ChatConfigurationScreen
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingsUiState
import com.mvlog.agentconfig.presentation.list.AgentConfigListScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch

internal class ChatConfigurationPresenter(
    private val screen: ChatConfigurationScreen,
    private val navigator: Navigator,
    private val observeConfigs: ObserveAgentConfigsUseCase,
    private val observeChatConfig: ObserveChatConfigUseCase,
    private val observeDefaultConfig: ObserveDefaultAgentConfigUseCase,
    private val setChatConfig: SetChatConfigUseCase,
) : Presenter<SettingsUiState> {

    @Composable
    override fun present(): SettingsUiState {
        val scope = rememberCoroutineScope()

        val configs by remember { observeConfigs() }.collectAsState(initial = emptyList())
        val effective by remember(screen.chatId) { observeChatConfig(screen.chatId) }.collectAsState(initial = null)
        val default by remember { observeDefaultConfig() }.collectAsState(initial = null)

        // `effective` is already resolved against the default, so equality is the whole test.
        val followsDefault = effective?.id == default?.id

        return SettingsUiState(
            title = TITLE,
            items = chatConfigurationItems(
                configs = configs,
                effectiveId = effective?.id?.value,
                defaultName = default?.name,
                followsDefault = followsDefault,
                onSelect = { configId -> scope.launch { setChatConfig(screen.chatId, configId) } },
                onManageModels = { navigator.goTo(AgentConfigListScreen) },
            ),
            onBack = navigator::pop,
        )
    }

    private companion object {
        const val TITLE = "Chat settings"
    }
}
