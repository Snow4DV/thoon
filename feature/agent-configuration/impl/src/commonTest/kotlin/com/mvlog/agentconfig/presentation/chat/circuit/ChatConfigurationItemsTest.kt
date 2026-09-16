package com.mvlog.agentconfig.presentation.chat.circuit

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatConfigurationItemsTest {

    private val configs = listOf(ollama("a", "Fast"), ollama("b", "Careful"))

    @Test
    fun theDefaultRowIsSelectedWhenTheChatHasNoOverride() {
        val items = items(overrideId = null)

        assertEquals(1, items.selectedCount())
        assertEquals("Use the app default", items.selectedTitles().single())
    }

    @Test
    fun theOverrideIsSelectedWhenTheChatHasOne() {
        val items = items(overrideId = "b")

        assertEquals(1, items.selectedCount())
        assertEquals("Careful", items.selectedTitles().single())
    }

    @Test
    fun anOverrideThatMatchesTheDefaultStillSelectsTheConfigRow() {
        val items = items(overrideId = "a", defaultName = "Fast")

        assertEquals(1, items.selectedCount())
        assertEquals("Fast", items.selectedTitles().single(), "pinning the default is still a pin")
    }

    @Test
    fun theDefaultRowSaysWhichConfigurationThatIs() {
        val items = items(overrideId = null, defaultName = "Fast")

        assertEquals("Currently Fast", items.filterIsInstance<SettingItem.Choice>().first().subtitle)
    }

    @Test
    fun withNoDefaultSetTheRowSaysSoRatherThanLookingEmpty() {
        val items = items(overrideId = null, defaultName = null)

        assertEquals("No default is set", items.filterIsInstance<SettingItem.Choice>().first().subtitle)
    }

    @Test
    fun manageModelsIsAlwaysLast() {
        val last = assertIs<SettingItem.Navigation>(items(overrideId = null).last())

        assertEquals("Manage models…", last.title)
    }

    @Test
    fun selectingTheDefaultRowClearsTheOverride() {
        var selected: AgentConfigId? = AgentConfigId("b")
        val items = chatConfigurationItems(
            configs = configs,
            overrideId = "b",
            defaultName = "Fast",
            onSelect = { selected = it },
            onManageModels = {},
        )

        items.filterIsInstance<SettingItem.Choice>().first().onSelect()

        assertTrue(selected == null, "the default row must pass null, which is what clears it")
    }

    private fun items(
        overrideId: String?,
        defaultName: String? = "Fast",
    ) = chatConfigurationItems(
        configs = configs,
        overrideId = overrideId,
        defaultName = defaultName,
        onSelect = {},
        onManageModels = {},
    )

    private fun List<SettingItem>.selectedCount() =
        filterIsInstance<SettingItem.Choice>().count { it.isSelected }

    private fun List<SettingItem>.selectedTitles() =
        filterIsInstance<SettingItem.Choice>().filter { it.isSelected }.map { it.title }

    private fun ollama(id: String, name: String) = AgentConfig.Ollama(
        id = AgentConfigId(id),
        name = name,
        modelId = "qwen3.8:27b",
        baseUrl = "http://10.0.2.2:11434",
    )
}
