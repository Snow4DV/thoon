package com.mvlog.agent.impl

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.model.ChatExecutionState
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.errorOrNull
import com.mvlog.agent.api.model.getOrNull
import com.mvlog.agent.impl.di.ThoonAgentComponentImpl
import com.mvlog.agent.impl.fake.TestAgentModule
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AgentConfigTest {

    @Test
    fun createdConfigIsObservableAndBecomesTheDefault() = configTest {
        val id = createConfig(openAi("Work")).expectSuccess()

        val configs = observeConfigs().first()
        assertEquals(1, configs.size)
        assertEquals("Work", configs.single().name)

        assertEquals(
            id,
            observeDefaultConfig().first()?.id,
            "the first configuration should become the default automatically",
        )
    }

    @Test
    fun editingReplacesFieldsButKeepsIdentity() = configTest {
        val id = createConfig(openAi("Work")).expectSuccess()

        updateConfig(id, openAi("Personal", modelId = "gpt-4o-mini")).expectSuccess()

        val updated = assertIs<AgentConfig.OpenAiCompatible>(observeConfig(id).first())
        assertEquals(id, updated.id, "editing must not mint a new configuration")
        assertEquals("Personal", updated.name)
        assertEquals("gpt-4o-mini", updated.modelId)
        assertEquals(1, observeConfigs().first().size, "editing must not add a row")
    }

    @Test
    fun removingTheDefaultLeavesNoDefaultRatherThanPickingOne() = configTest {
        val first = createConfig(openAi("First")).expectSuccess()
        createConfig(openAi("Second")).expectSuccess()
        assertEquals(first, observeDefaultConfig().first()?.id)

        deleteConfig(first)

        assertNull(
            observeDefaultConfig().first(),
            "a silently reassigned default would be invisible to the user",
        )
        assertEquals(1, observeConfigs().first().size)
    }

    @Test
    fun chatOverrideWinsOverDefaultAndClearingItFallsBack() = configTest {
        val default = createConfig(openAi("Default")).expectSuccess()
        val override = createConfig(openAi("Override")).expectSuccess()
        setDefaultConfig(default).expectSuccess()

        val chatId = createChat(configId = null)
        assertEquals(default, observeChatConfig(chatId).first()?.id)

        setChatConfig(chatId, override).expectSuccess()
        assertEquals(override, observeChatConfig(chatId).first()?.id)

        setChatConfig(chatId, null).expectSuccess()
        assertEquals(default, observeChatConfig(chatId).first()?.id)
    }

    @Test
    fun deletingAConfigDetachesTheChatsThatSelectedIt() = configTest {
        val default = createConfig(openAi("Default")).expectSuccess()
        val override = createConfig(openAi("Override")).expectSuccess()
        setDefaultConfig(default).expectSuccess()

        val chatId = createChat(configId = override)
        assertEquals(override, observeChatConfig(chatId).first()?.id)

        deleteConfig(override)

        assertEquals(
            default,
            observeChatConfig(chatId).first()?.id,
            "a chat pinned to a deleted configuration should fall back, not break",
        )
    }

    @Test
    fun chatCanBePinnedAtCreation() = configTest {
        val pinned = createConfig(openAi("Pinned")).expectSuccess()
        val chatId = createChat(configId = pinned)

        assertEquals(pinned, observeChatConfig(chatId).first()?.id)
    }

    @Test
    fun invalidDraftsAreRejectedWithTheOffendingField() = configTest {
        assertEquals(
            AgentConfigError.BlankName,
            createConfig(openAi(name = "  ")).errorOrNull(),
        )
        assertEquals(
            AgentConfigError.BlankModelId,
            createConfig(openAi(modelId = "")).errorOrNull(),
        )
        assertEquals(
            AgentConfigError.MissingApiKey,
            createConfig(openAi(apiKey = "")).errorOrNull(),
        )

        val malformed = createConfig(openAi(baseUrl = "not-a-url")).errorOrNull()
        assertIs<AgentConfigError.MalformedBaseUrl>(malformed)

        assertTrue(
            observeConfigs().first().isEmpty(),
            "a rejected draft must not be stored",
        )
    }

    @Test
    fun privateHostsAreAcceptedAsBaseUrls() = configTest {
        // The whole point of a custom endpoint is reaching hosts a strict validator would reject.
        createConfig(openAi("LM Studio", baseUrl = "http://192.168.1.5:1234/v1")).expectSuccess()
        createConfig(openAi("Ollama", baseUrl = "http://localhost:11434/v1")).expectSuccess()

        assertEquals(2, observeConfigs().first().size)
    }

    @Test
    fun updatingAMissingConfigReportsNotFound() = configTest {
        val error = updateConfig(AgentConfigId("nope"), openAi("Ghost")).errorOrNull()
        assertEquals(AgentConfigError.NotFound(AgentConfigId("nope")), error)
    }

    @Test
    fun promptingWithNoConfigurationFailsVisiblyInsteadOfAnswering() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        try {
            // echoRuns = false: this must go through the real factory, which has nothing to serve.
            val component = ThoonAgentComponentImpl(
                module = TestAgentModule(agentScope = scope),
            )
            component.startAgentRuntimeUseCase().invoke()

            val chatId = component.createChatUseCase()(configId = null)
            val states = mutableListOf<com.mvlog.agent.api.model.ChatState>()
            scope.launch { component.observeChatUseCase()(chatId).collect(states::add) }
            advanceUntilIdle()

            component.sendPromptUseCase()(chatId, "hello")
            advanceUntilIdle()

            val finished = states.last()
            val failure = assertIs<ChatExecutionState.Failed>(
                finished.execution,
                "an unconfigured agent must report a failure, not answer",
            )
            assertTrue(
                failure.message?.contains("No agent configuration") == true,
                "the failure should say what to fix, was: ${failure.message}",
            )
            assertTrue(
                finished.items.none { it is com.mvlog.agent.api.model.ChatItem.AssistantMessage },
                "nothing should have answered the prompt",
            )
        } finally {
            scope.cancel()
        }
    }

    private fun openAi(
        name: String = "Config",
        modelId: String = "gpt-4o",
        apiKey: String = "sk-test",
        baseUrl: String? = null,
    ) = AgentConfigDraft.OpenAiCompatible(
        name = name,
        modelId = modelId,
        apiKey = apiKey,
        baseUrl = baseUrl,
    )

    private fun <T> AgentConfigResult<T>.expectSuccess(): T =
        getOrNull() ?: error("expected success but was ${(this as AgentConfigResult.Failure).error}")

    private class Fixture(component: ThoonAgentComponentImpl) {
        val createChat = component.createChatUseCase()
        val observeChatConfig = component.observeChatConfigUseCase()
        val setChatConfig = component.setChatConfigUseCase()
        val createConfig = component.createAgentConfigUseCase()
        val updateConfig = component.updateAgentConfigUseCase()
        val deleteConfig = component.deleteAgentConfigUseCase()
        val setDefaultConfig = component.setDefaultAgentConfigUseCase()
        val observeConfigs = component.observeAgentConfigsUseCase()
        val observeConfig = component.observeAgentConfigUseCase()
        val observeDefaultConfig = component.observeDefaultAgentConfigUseCase()
    }

    private fun configTest(
        body: suspend Fixture.() -> Unit,
    ): TestResult = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        try {
            val component = ThoonAgentComponentImpl(
                module = TestAgentModule(agentScope = scope, echoRuns = true),
            )
            Fixture(component).body()
        } finally {
            scope.cancel()
        }
    }


}
