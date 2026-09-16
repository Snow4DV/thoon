package com.mvlog.agentconfig.presentation

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agentconfig.api.AgentConfigurationScreen
import com.mvlog.agentconfig.api.ChatConfigurationScreen
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private data object ForeignScreen : Screen

/**
 * One factory answers for four screens. What matters is that it claims exactly those and declines
 * everything else — a factory answering for a foreign screen would shadow whichever owns it.
 */
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
        observeChatConfig = { flowOf(null) },
        setChatConfig = { _, _ -> AgentConfigResult.Success(Unit) },
        localEngineProvider = { emptyList() },
    )
}
