package com.mvlog.agentconfig.presentation

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.agentconfig.api.ChatConfigurationScreen
import com.mvlog.agentconfig.presentation.chat.circuit.ChatConfigurationPresenter
import com.mvlog.agentconfig.presentation.configuration.circuit.AgentConfigurationPresenter
import com.mvlog.agentconfig.presentation.editor.AgentConfigEditorScreen
import com.mvlog.agentconfig.presentation.editor.circuit.AgentConfigEditorPresenter
import com.mvlog.agentconfig.presentation.list.AgentConfigListScreen
import com.mvlog.agentconfig.presentation.list.circuit.AgentConfigListPresenter
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.flowOf

private data object ForeignScreen : Screen

class AgentConfigurationFactoriesTest {

    @Test
    fun thePresenterFactoryClaimsEveryScreenThisFeatureOwns() {
        val factory = presenterFactory()

        assertIs<AgentConfigurationPresenter>(factory.presenter(AgentConfigurationScreen()))
        assertIs<AgentConfigListPresenter>(factory.presenter(AgentConfigListScreen))
        assertIs<AgentConfigEditorPresenter>(factory.presenter(AgentConfigEditorScreen()))
        assertIs<ChatConfigurationPresenter>(factory.presenter(ChatConfigurationScreen(ChatId("chat-1"))))
    }

    @Test
    fun thePresenterFactoryDeclinesForeignScreens() {
        assertNull(
            presenterFactory().presenter(ForeignScreen),
            "claiming a screen it does not own would shadow the factory that does",
        )
    }

    @Test
    fun theUiFactoryClaimsOnlyItsOwnScreens() {
        val factory = AgentConfigurationUiFactory()

        assertNotNull(factory.create(AgentConfigurationScreen(), CircuitContext.EMPTY))
        assertNotNull(factory.create(AgentConfigListScreen, CircuitContext.EMPTY))
        assertNotNull(factory.create(AgentConfigEditorScreen(), CircuitContext.EMPTY))
        assertNotNull(factory.create(ChatConfigurationScreen(ChatId("chat-1")), CircuitContext.EMPTY))
        assertNull(factory.create(ForeignScreen, CircuitContext.EMPTY))
    }

    private fun AgentConfigurationPresenterFactory.presenter(screen: Screen) =
        create(screen, Navigator.NoOp, CircuitContext.EMPTY)

    private fun presenterFactory() = AgentConfigurationPresenterFactory(
        observeConfigs = { flowOf(emptyList()) },
        getConfig = { null },
        observeDefaultConfig = { flowOf(null) },
        createConfig = { AgentConfigResult.Success(AgentConfigId("new")) },
        updateConfig = { _, _ -> AgentConfigResult.Success(Unit) },
        deleteConfig = { },
        setDefaultConfig = { AgentConfigResult.Success(Unit) },
        observeChatConfigOverride = { flowOf(null) },
        setChatConfig = { _, _ -> AgentConfigResult.Success(Unit) },
        observeToolApprovalRules = { flowOf(emptyList()) },
        addToolApprovalRule = { _, _ -> },
        revokeToolApprovalRule = { },
        getApprovalGatedTools = { emptyList() },
        localEngineProvider = { emptyList() },
    )
}
