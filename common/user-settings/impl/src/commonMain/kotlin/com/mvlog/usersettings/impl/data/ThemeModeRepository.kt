package com.mvlog.usersettings.impl.data

import com.mvlog.sharedpreferences.api.KeyValueStore
import com.mvlog.usersettings.api.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class ThemeModeRepository(private val store: KeyValueStore) {

    private val mode = MutableStateFlow(read())

    val themeMode: StateFlow<ThemeMode> = mode.asStateFlow()

    fun set(themeMode: ThemeMode) {
        store.putString(KEY, themeMode.name)
        mode.value = themeMode
    }

    private fun read(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == store.getString(KEY) } ?: ThemeMode.System

    private companion object {
        const val KEY = "theme_mode"
    }
}
