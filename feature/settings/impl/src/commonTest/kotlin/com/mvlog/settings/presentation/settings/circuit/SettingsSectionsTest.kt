package com.mvlog.settings.presentation.settings.circuit

import com.mvlog.settings.api.SettingsSection
import com.mvlog.settings.presentation.common.settings.ui.SettingItem
import com.mvlog.usersettings.api.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsSectionsTest {

    @Test
    fun theRootOffersEverySection() {
        val items = settingsRootItems(onOpen = {})

        assertEquals(SettingsSection.entries.size, items.size)
        assertEquals(
            SettingsSection.entries.map { it.title() },
            items.filterIsInstance<SettingItem.Navigation>().map { it.title },
        )
    }

    @Test
    fun everySectionShowsSomething() {
        SettingsSection.entries.forEach { section ->
            assertTrue(
                settingsSectionItems(section, onOpenModelAndProvider = {}).isNotEmpty(),
                "$section renders nothing",
            )
        }
    }

    @Test
    fun anEmptySectionExplainsWhyRatherThanJustBeingBlank() {
        val agent = settingsSectionItems(SettingsSection.Agent, onOpenModelAndProvider = {})
        val placeholder = agent.filterIsInstance<SettingItem.Placeholder>().single()

        assertTrue(
            "agent_settings" in placeholder.text,
            "the placeholder should say what is missing, was: ${placeholder.text}",
        )
    }

    @Test
    fun theAppearanceSectionSelectsExactlyTheCurrentMode() {
        ThemeMode.entries.forEach { mode ->
            val choices = settingsSectionItems(
                SettingsSection.Appearance,
                onOpenModelAndProvider = {},
                themeMode = mode,
            ).filterIsInstance<SettingItem.Choice>()

            assertEquals(ThemeMode.entries.size, choices.size, "every mode must be offered")
            assertEquals(
                listOf(mode.name),
                choices.filter { it.isSelected }.map { it.title },
                "exactly the current mode must be selected",
            )
        }
    }

    @Test
    fun choosingAModeReportsIt() {
        var chosen: ThemeMode? = null
        val choices = settingsSectionItems(
            SettingsSection.Appearance,
            onOpenModelAndProvider = {},
            onSelectThemeMode = { chosen = it },
        ).filterIsInstance<SettingItem.Choice>()

        choices.single { it.title == "Dark" }.onSelect()

        assertEquals(ThemeMode.Dark, chosen, "tapping Dark must store Dark")
    }

    @Test
    fun modelAndProviderLeadsToItsOwnScreen() {
        var opened = false
        val items = settingsRootItems(onOpen = { if (it == SettingsSection.ModelAndProvider) opened = true })

        (items.first() as SettingItem.Navigation).onClick()

        assertTrue(opened, "the first root row must be Model & Provider and must open it")
    }
}
