package com.mvlog.agentconfig.presentation.editor.circuit

import com.mvlog.agent.api.engine.LocalEngine
import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agentconfig.presentation.editor.circuit.AgentConfigEditorState.Editing
import com.mvlog.agentconfig.presentation.editor.circuit.AgentConfigEditorState.Loading
import com.mvlog.agentconfig.presentation.editor.circuit.AgentConfigEditorState.Missing
import com.mvlog.agentconfig.presentation.editor.ui.Rejection
import com.mvlog.agentconfig.presentation.editor.ui.mapper.AgentConfigForm
import com.mvlog.agentconfig.presentation.editor.ui.mapper.ConfigField
import com.mvlog.agentconfig.presentation.editor.ui.mapper.ConfigKind
import com.mvlog.agentconfig.presentation.editor.ui.mapper.message
import com.mvlog.agentconfig.presentation.editor.ui.mapper.toForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AgentConfigEditorStateHolderTest {

    private val saved = AgentConfig.Ollama(
        id = AgentConfigId("cfg"),
        name = "Llama",
        modelId = "llama3",
        baseUrl = "http://10.0.2.2:11434",
    )

    private fun newConfig() = AgentConfigEditorStateHolder(configId = null)

    private fun existingConfig() = AgentConfigEditorStateHolder(configId = saved.id)

    private fun rejectedOnName() = newConfig().apply { rejected(AgentConfigError.BlankName) }

    @Test
    fun aNewConfigurationStartsEditableAndAnExistingOneStartsLoading() {
        assertEquals(Editing(AgentConfigForm()), newConfig().value)
        assertEquals(Loading, existingConfig().value)
    }

    @Test
    fun aLoadSeedsTheFormOrReportsTheConfigurationMissing() {
        assertEquals(Editing(saved.toForm()), existingConfig().apply { loaded(saved.id, saved) }.value)
        assertEquals(Missing(saved.id), existingConfig().apply { loaded(saved.id, null) }.value)
    }

    @Test
    fun aLoadLandingAfterTypingDoesNotOverwriteTheDraft() {
        val holder = newConfig().apply { edit { copy(name = "typed") } }
        holder.loaded(saved.id, saved)
        assertEquals(
            "typed",
            assertIs<Editing>(holder.value).form.name,
            "a load landing after rotation must not overwrite what was typed before it",
        )
    }

    @Test
    fun aRejectionAboutAFieldLandsUnderItAndNotInTheBanner() {
        val editing = assertIs<Editing>(rejectedOnName().value)
        assertEquals(
            Rejection.Field(ConfigField.Name, AgentConfigError.BlankName.message()),
            editing.rejection,
            "a field error is not also a banner",
        )
        assertFalse(editing.isBusy, "a rejection settles the write")
    }

    @Test
    fun aRejectionAboutNoFieldIsTheBanner() {
        val holder = newConfig().apply { rejected(AgentConfigError.NotFound(saved.id)) }
        assertIs<Rejection.Screen>(assertIs<Editing>(holder.value).rejection)
    }

    @Test
    fun anEditClearsTheRejectionInTheSameTransition() {
        val holder = rejectedOnName().apply { edit { copy(name = "x") } }
        val editing = assertIs<Editing>(holder.value)
        assertEquals("x", editing.form.name)
        assertNull(editing.rejection, "a rejection must not outlive the edit that answers it")
    }

    @Test
    fun aRejectionKeepsWhatWasTypedWhileTheSaveWasInFlight() {
        val holder = newConfig().apply {
            busy()
            edit { copy(name = "typed later") }
            rejected(AgentConfigError.BlankModelId)
        }
        assertEquals("typed later", assertIs<Editing>(holder.value).form.name)
    }

    @Test
    fun nothingCanBeStartedWhileAWriteIsInFlight() {
        val holder = newConfig().apply { busy() }
        assertNull(holder.idleForm())
        assertNull(holder.saveableForm(emptyList()))
        assertFalse(holder.canSave(emptyList()))

        holder.settled()
        assertNotNull(holder.idleForm())
        assertTrue(holder.canSave(emptyList()))
    }

    @Test
    fun anOnDeviceFormIsSaveableOnlyWhenAnEngineExists() {
        val holder = newConfig().apply { edit { copy(kind = ConfigKind.Local) } }
        assertFalse(holder.canSave(emptyList()), "an empty engine registry must be visible, not a broken profile")
        assertTrue(holder.canSave(listOf(LocalEngine(id = "mlx", displayName = "MLX"))))
    }

    @Test
    fun transitionsWithoutAFormAreNoOps() {
        val missing = existingConfig().apply { loaded(saved.id, null) }
        val before = missing.value
        missing.edit { copy(name = "x") }
        missing.busy()
        missing.settled()
        missing.rejected(AgentConfigError.BlankName)
        assertEquals(before, missing.value)
        assertNull(missing.idleForm())
        assertFalse(missing.canSave(emptyList()))

        val loading = existingConfig()
        loading.edit { copy(name = "x") }
        assertEquals(Loading, loading.value, "typing cannot start before the load has landed")
    }
}
