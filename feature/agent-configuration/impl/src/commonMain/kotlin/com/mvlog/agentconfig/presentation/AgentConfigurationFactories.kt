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
import com.mvlog.agentconfig.presentation.ui.AgentConfigEditorUi
import com.mvlog.agentconfig.presentation.ui.SettingsUi
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.runtime.ui.ui

/**
 * Resolves every screen this feature owns.
 *
 * One factory for four screens: a Circuit factory claims whatever its `when` covers, and returning
 * null for anything else is the contract rather than a fallback.
 */
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
    /**
     * Read when the editor is created, not when the factory is: engines are registered by
     * initializers, and the registry is meant to be consulted every time the editor opens.
     */
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

/**
 * Three of the four screens share one composable, which is the point of the settings renderer: the
 * root, a section and per-chat configuration differ only in the items they carry.
 */
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
