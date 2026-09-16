package com.mvlog.agentconfig.presentation

import com.mvlog.agent.api.engine.LocalEngineProvider
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase
import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.agentconfig.api.ChatConfigurationScreen
import com.mvlog.agentconfig.presentation.chat.circuit.ChatConfigurationPresenter
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingsUi
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingsUiState
import com.mvlog.agentconfig.presentation.configuration.circuit.AgentConfigurationPresenter
import com.mvlog.agentconfig.presentation.editor.AgentConfigEditorScreen
import com.mvlog.agentconfig.presentation.editor.circuit.AgentConfigEditorPresenter
import com.mvlog.agentconfig.presentation.editor.ui.AgentConfigEditorUi
import com.mvlog.agentconfig.presentation.editor.ui.AgentConfigEditorUiState
import com.mvlog.agentconfig.presentation.list.AgentConfigListScreen
import com.mvlog.agentconfig.presentation.list.circuit.AgentConfigListPresenter
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui

internal class AgentConfigurationPresenterFactory(
    private val observeConfigs: ObserveAgentConfigsUseCase,
    private val getConfig: GetAgentConfigUseCase,
    private val observeDefaultConfig: ObserveDefaultAgentConfigUseCase,
    private val createConfig: CreateAgentConfigUseCase,
    private val updateConfig: UpdateAgentConfigUseCase,
    private val deleteConfig: DeleteAgentConfigUseCase,
    private val setDefaultConfig: SetDefaultAgentConfigUseCase,
    private val observeChatConfig: ObserveChatConfigUseCase,
    private val setChatConfig: SetChatConfigUseCase,
    /** Read per editor, not per factory, so a late-registered engine shows on the next open. */
    private val localEngineProvider: LocalEngineProvider,
) : Presenter.Factory {

    override fun create(
        screen: Screen,
        navigator: Navigator,
        context: CircuitContext,
    ): Presenter<*>? = when (screen) {
        is AgentConfigurationScreen -> AgentConfigurationPresenter(screen, navigator)

        is AgentConfigListScreen -> AgentConfigListPresenter(
            navigator = navigator,
            observeConfigs = observeConfigs,
            observeDefaultConfig = observeDefaultConfig,
        )

        is AgentConfigEditorScreen -> AgentConfigEditorPresenter(
            screen = screen,
            navigator = navigator,
            localEngines = localEngineProvider.engines(),
            getConfig = getConfig,
            observeDefaultConfig = observeDefaultConfig,
            createConfig = createConfig,
            updateConfig = updateConfig,
            deleteConfig = deleteConfig,
            setDefaultConfig = setDefaultConfig,
        )

        is ChatConfigurationScreen -> ChatConfigurationPresenter(
            screen = screen,
            navigator = navigator,
            observeConfigs = observeConfigs,
            observeChatConfig = observeChatConfig,
            observeDefaultConfig = observeDefaultConfig,
            setChatConfig = setChatConfig,
        )

        else -> null
    }
}

internal class AgentConfigurationUiFactory : Ui.Factory {

    override fun create(screen: Screen, context: CircuitContext): Ui<*>? = when (screen) {
        is AgentConfigurationScreen,
        is AgentConfigListScreen,
        is ChatConfigurationScreen,
        -> ui<SettingsUiState> { state, modifier -> SettingsUi(state, modifier) }

        is AgentConfigEditorScreen ->
            ui<AgentConfigEditorUiState> { state, modifier -> AgentConfigEditorUi(state, modifier) }

        else -> null
    }
}
