package com.mvlog.agentconfig.presentation

import com.mvlog.agentconfig.api.ConfigurationSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationSectionsTest {

    @Test
    fun theRootOffersEverySection() {
        // Built from `entries`, so this catches a section added to the enum and forgotten here.
        // Its *contents* are already compiler-enforced by an exhaustive `when`.
        val items = configurationRootItems(onOpen = {})

        assertEquals(ConfigurationSection.entries.size, items.size)
        assertEquals(
            ConfigurationSection.entries.map { it.title() },
            items.filterIsInstance<SettingItem.Navigation>().map { it.title },
        )
    }

    @Test
    fun everySectionShowsSomething() {
        // A section that rendered nothing at all would look like a failed load rather than an
        // unfinished feature.
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
        // It is CRUD, not a settings list, so the root row skips the generic section entirely.
        var opened = false
        val items = configurationRootItems(onOpen = { if (it == ConfigurationSection.ModelAndProvider) opened = true })

        (items.first() as SettingItem.Navigation).onClick()

        assertTrue(opened, "the first root row must be Model & Provider and must open it")
    }
}
