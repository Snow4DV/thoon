package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agentconfig.api.ChatConfigurationScreen
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch

/**
 * Which model one conversation uses.
 *
 * The settings renderer's second consumer, and the reason it earns its place: this is a single-select
 * list plus a link, which is settings-shaped, so it needed one new row type rather than a screen of
 * its own.
 *
 * `observeChatConfig` reports the *effective* configuration — the override if one is set, otherwise
 * the default — so the override itself is read from the chat's own record via the configs list. What
 * matters for selection is whether the chat has an override at all, which is why the default row is
 * selected precisely when it does not.
 */
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
        val chatId = screen.chatId

        val configs by remember { observeConfigs() }.collectAsState(initial = emptyList())
        val effective by remember(chatId) { observeChatConfig(chatId) }.collectAsState(initial = null)
        val default by remember { observeDefaultConfig() }.collectAsState(initial = null)

        // The chat follows the default exactly when what it resolves to *is* the default. Comparing
        // resolved values rather than storing a separate flag keeps this honest when the default
        // itself changes underneath the screen.
        val followsDefault = effective?.id == default?.id

        return SettingsUiState(
            title = TITLE,
            items = chatConfigurationItems(
                configs = configs,
                effectiveId = effective?.id?.value,
                defaultName = default?.name,
                followsDefault = followsDefault,
                onSelect = { configId -> scope.launch { setChatConfig(chatId, configId) } },
                onManageModels = { navigator.goTo(AgentConfigListScreen) },
            ),
            onBack = navigator::pop,
        )
    }

    private companion object {
        const val TITLE = "Chat settings"
    }
}
