package com.mvlog.agentconfig.presentation

import com.mvlog.agent.api.engine.LocalEngine
import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase
import com.mvlog.agentconfig.presentation.AgentConfigEditorUiEvent.Ui
import com.mvlog.agentconfig.presentation.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.mapper.ConfigField
import com.mvlog.agentconfig.presentation.mapper.ConfigKind
import com.mvlog.agentconfig.presentation.mapper.message
import com.mvlog.agentconfig.presentation.mapper.toForm
import com.slack.circuit.test.CircuitReceiveTurbine
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Driven through the event sink on emitted states, as `docs/circuit/PRESENTER.md` prescribes.
 *
 * `awaitItem()` is distinct-until-changed, but the sink is a fresh lambda each composition, so a
 * recomposition can emit an otherwise-identical state. Assertions therefore wait for the state
 * they describe rather than counting emissions.
 */
class AgentConfigEditorPresenterTest {

    private val saved = AgentConfig.Ollama(
        id = AgentConfigId("cfg-1"),
        name = "Local llama",
        modelId = "llama3",
        baseUrl = "http://10.0.2.2:11434",
    )

    @Test
    fun aNewConfigurationStartsAsAnEmptyEditableForm() = runTest {
        presenter().test {
            val data = awaitData()
            assertEquals(AgentConfigForm(), data.form)
            assertNull(data.existing, "a new configuration has no saved standing to show")
            assertTrue(data.canSave, "an empty remote form is saveable; validation is the use case's job")
        }
    }

    @Test
    fun anExistingConfigurationIsLoadedIntoTheFormAfterALoadingFrame() = runTest {
        presenter(screen = AgentConfigEditorScreen(saved.id)).test {
            assertIs<AgentConfigEditorUiState.Loading>(
                awaitItem(),
                "an existing configuration must not render as a blank form while it loads",
            )
            assertEquals(saved.toForm(), awaitData().form)
        }
    }

    @Test
    fun aConfigurationThatNoLongerExistsIsAnError() = runTest {
        presenter(
            screen = AgentConfigEditorScreen(AgentConfigId("gone")),
            getConfig = { null },
        ).test {
            assertIs<AgentConfigEditorUiState.Loading>(awaitItem())
            val error = awaitUntil<AgentConfigEditorUiState.Error> { true }
            assertEquals(AgentConfigError.NotFound(AgentConfigId("gone")).message(), error.message)
        }
    }

    @Test
    fun aRejectedSaveLandsUnderTheFieldThatCausedIt() = runTest {
        presenter(createConfig = { AgentConfigResult.Failure(AgentConfigError.BlankName) }).test {
            awaitData().eventSink(Ui.SaveClicked)

            val rejected = awaitData { it.rejection != null }
            assertEquals(Rejection.Field(ConfigField.Name, AgentConfigError.BlankName.message()), rejected.rejection)
        }
    }

    @Test
    fun editingAnyFieldClearsTheRejection() = runTest {
        presenter(createConfig = { AgentConfigResult.Failure(AgentConfigError.BlankName) }).test {
            awaitData().eventSink(Ui.SaveClicked)
            awaitData { it.rejection != null }.eventSink(Ui.FieldChanged(ConfigField.Name, "x"))

            val edited = awaitData { it.form.name == "x" }
            assertNull(edited.rejection, "a rejection must not outlive the edit that answers it")
        }
    }

    @Test
    fun aSuccessfulSavePopsTheScreen() = runTest {
        val navigator = FakeNavigator(AgentConfigEditorScreen())
        presenter(navigator = navigator).test {
            awaitData().eventSink(Ui.SaveClicked)
            navigator.awaitPop()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aSecondTapWhileSavingDoesNotSaveTwice() = runTest {
        val gate = CompletableDeferred<Unit>()
        var creates = 0
        val navigator = FakeNavigator(AgentConfigEditorScreen())
        presenter(
            navigator = navigator,
            createConfig = {
                creates++
                gate.await()
                AgentConfigResult.Success(AgentConfigId("new"))
            },
        ).test {
            awaitData().eventSink(Ui.SaveClicked)
            val busy = awaitData { it.isBusy }
            assertFalse(busy.canSave, "the save button is held while a save is in flight")

            busy.eventSink(Ui.SaveClicked)
            gate.complete(Unit)
            navigator.awaitPop()

            assertEquals(1, creates, "two taps must not create two configurations")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun makingTheDefaultIsReflectedFromStorageNotFromALocalFlag() = runTest {
        val default = MutableStateFlow<AgentConfig?>(null)
        presenter(
            screen = AgentConfigEditorScreen(saved.id),
            observeDefault = default,
            setDefaultConfig = { id ->
                default.value = saved.copy(id = id)
                AgentConfigResult.Success(Unit)
            },
        ).test {
            val data = awaitData()
            assertEquals(Existing(isDefault = false), data.existing)

            data.eventSink(Ui.MakeDefaultClicked)
            awaitData { it.existing?.isDefault == true }
        }
    }

    @Test
    fun anOnDeviceConfigurationCannotBeSavedWithoutAnEngine() = runTest {
        presenter(engines = emptyList()).test {
            awaitData().eventSink(Ui.KindSelected(ConfigKind.Local))
            val local = awaitData { it.form.kind == ConfigKind.Local }
            assertFalse(local.canSave, "an empty engine registry must be visible, not a broken profile")
        }
    }

    @Test
    fun choosingAnEngineIsWrittenToTheForm() = runTest {
        val engine = LocalEngine(id = "mlx", displayName = "MLX")
        presenter(engines = listOf(engine)).test {
            awaitData().eventSink(Ui.KindSelected(ConfigKind.Local))
            awaitData { it.form.kind == ConfigKind.Local }.eventSink(Ui.EngineSelected(engine.id))

            val chosen = awaitData { it.form.engineId == engine.id }
            assertTrue(chosen.canSave)
        }
    }

    private fun presenter(
        screen: AgentConfigEditorScreen = AgentConfigEditorScreen(),
        navigator: FakeNavigator = FakeNavigator(screen),
        engines: List<LocalEngine> = emptyList(),
        getConfig: GetAgentConfigUseCase = GetAgentConfigUseCase { saved },
        observeDefault: MutableStateFlow<AgentConfig?> = MutableStateFlow(null),
        createConfig: CreateAgentConfigUseCase =
            CreateAgentConfigUseCase { AgentConfigResult.Success(AgentConfigId("new")) },
        updateConfig: UpdateAgentConfigUseCase =
            UpdateAgentConfigUseCase { _, _ -> AgentConfigResult.Success(Unit) },
        deleteConfig: DeleteAgentConfigUseCase = DeleteAgentConfigUseCase { },
        setDefaultConfig: SetDefaultAgentConfigUseCase =
            SetDefaultAgentConfigUseCase { AgentConfigResult.Success(Unit) },
    ) = AgentConfigEditorPresenter(
        screen = screen,
        navigator = navigator,
        localEngines = engines,
        getConfig = getConfig,
        observeDefaultConfig = { observeDefault },
        createConfig = createConfig,
        updateConfig = updateConfig,
        deleteConfig = deleteConfig,
        setDefaultConfig = setDefaultConfig,
    )

    private suspend fun CircuitReceiveTurbine<AgentConfigEditorUiState>.awaitData(
        predicate: (AgentConfigEditorUiState.Data) -> Boolean = { true },
    ): AgentConfigEditorUiState.Data = awaitUntil(predicate)

    private suspend inline fun <reified S : AgentConfigEditorUiState>
        CircuitReceiveTurbine<AgentConfigEditorUiState>.awaitUntil(
        predicate: (S) -> Boolean,
    ): S {
        while (true) {
            val item = awaitItem()
            if (item is S && predicate(item)) return item
        }
    }
}
