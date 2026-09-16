package com.mvlog.agentconfig.presentation.configuration.circuit

import com.mvlog.agentconfig.api.ConfigurationSection
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationSectionsTest {

    @Test
    fun theRootOffersEverySection() {
        val items = configurationRootItems(onOpen = {})

        assertEquals(ConfigurationSection.entries.size, items.size)
        assertEquals(
            ConfigurationSection.entries.map { it.title() },
            items.filterIsInstance<SettingItem.Navigation>().map { it.title },
        )
    }

    @Test
    fun everySectionShowsSomething() {
        ConfigurationSection.entries.forEach { section ->
            assertTrue(
                configurationSectionItems(section, onOpenModelAndProvider = {}).isNotEmpty(),
                "$section renders nothing",
            )
        }
    }

    @Test
    fun anEmptySectionExplainsWhyRatherThanJustBeingBlank() {
        val agent = configurationSectionItems(ConfigurationSection.Agent, onOpenModelAndProvider = {})
        val placeholder = agent.filterIsInstance<SettingItem.Placeholder>().single()

        assertTrue(
            "agent_settings" in placeholder.text,
            "the placeholder should say what is missing, was: ${placeholder.text}",
        )
    }

    @Test
    fun modelAndProviderLeadsToItsOwnScreen() {
        var opened = false
        val items = configurationRootItems(onOpen = { if (it == ConfigurationSection.ModelAndProvider) opened = true })

        (items.first() as SettingItem.Navigation).onClick()

        assertTrue(opened, "the first root row must be Model & Provider and must open it")
    }
}
