package com.mvlog.settings.presentation

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.model.ChatId
import com.mvlog.settings.api.SettingsScreen
import com.mvlog.settings.api.ChatConfigurationScreen
import com.mvlog.settings.presentation.chat.circuit.ChatConfigurationPresenter
import com.mvlog.settings.presentation.settings.circuit.SettingsPresenter
import com.mvlog.settings.presentation.editor.AgentConfigEditorScreen
import com.mvlog.settings.presentation.editor.circuit.AgentConfigEditorPresenter
import com.mvlog.settings.presentation.list.AgentConfigListScreen
import com.mvlog.settings.presentation.list.circuit.AgentConfigListPresenter
import com.mvlog.usersettings.api.model.ThemeMode
import com.slack.circuit.runtime.CircuitContext
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.screen.Screen
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

private data object ForeignScreen : Screen

class SettingsFactoriesTest {

    @Test
    fun thePresenterFactoryClaimsEveryScreenThisFeatureOwns() {
        val factory = presenterFactory()

        assertIs<SettingsPresenter>(factory.presenter(SettingsScreen()))
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
        val factory = SettingsUiFactory()

        assertNotNull(factory.create(SettingsScreen(), CircuitContext.EMPTY))
        assertNotNull(factory.create(AgentConfigListScreen, CircuitContext.EMPTY))
        assertNotNull(factory.create(AgentConfigEditorScreen(), CircuitContext.EMPTY))
        assertNotNull(factory.create(ChatConfigurationScreen(ChatId("chat-1")), CircuitContext.EMPTY))
        assertNull(factory.create(ForeignScreen, CircuitContext.EMPTY))
    }

    private fun SettingsPresenterFactory.presenter(screen: Screen) =
        create(screen, Navigator.NoOp, CircuitContext.EMPTY)

    private fun presenterFactory() = SettingsPresenterFactory(
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
        observeThemeMode = { MutableStateFlow(ThemeMode.System) },
        setThemeMode = { },
    )
}
