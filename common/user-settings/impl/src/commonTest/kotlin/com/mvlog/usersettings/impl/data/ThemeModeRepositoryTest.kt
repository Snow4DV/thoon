package com.mvlog.usersettings.impl.data

import com.mvlog.usersettings.api.model.ThemeMode
import com.mvlog.usersettings.impl.fake.InMemoryKeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeRepositoryTest {

    @Test
    fun anEmptyStoreFollowsTheSystem() {
        val repository = ThemeModeRepository(InMemoryKeyValueStore())

        assertEquals(
            ThemeMode.System,
            repository.themeMode.value,
            "a fresh install must follow the system",
        )
    }

    @Test
    fun anUnreadableStoredValueFollowsTheSystem() {
        val store = InMemoryKeyValueStore().apply { values["theme_mode"] = "Sepia" }

        val repository = ThemeModeRepository(store)

        assertEquals(
            ThemeMode.System,
            repository.themeMode.value,
            "an unknown value must not crash or pick a side",
        )
    }

    @Test
    fun aSetModeIsPersistedAndEmitted() {
        val store = InMemoryKeyValueStore()
        val repository = ThemeModeRepository(store)

        repository.set(ThemeMode.Dark)

        assertEquals(
            ThemeMode.Dark,
            repository.themeMode.value,
            "observers must see the change at once",
        )
        assertEquals("Dark", store.values["theme_mode"], "the choice must survive a restart")
    }

    @Test
    fun aNewRepositoryReadsTheModeAnEarlierOneWrote() {
        val store = InMemoryKeyValueStore()
        ThemeModeRepository(store).set(ThemeMode.Light)

        val restarted = ThemeModeRepository(store)

        assertEquals(
            ThemeMode.Light,
            restarted.themeMode.value,
            "the first frame after a restart must use the saved mode",
        )
    }
}
