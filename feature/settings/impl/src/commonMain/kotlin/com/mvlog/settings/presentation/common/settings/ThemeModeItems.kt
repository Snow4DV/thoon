package com.mvlog.settings.presentation.common.settings

import com.mvlog.settings.presentation.common.settings.ui.SettingItem
import com.mvlog.usersettings.api.model.ThemeMode
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal fun themeModeItems(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
): PersistentList<SettingItem> = ThemeMode.entries
    .map { mode ->
        SettingItem.Choice(
            title = mode.title(),
            subtitle = if (mode == ThemeMode.System) "Follow the device setting" else null,
            isSelected = mode == selected,
            onSelect = { onSelect(mode) },
        )
    }
    .toPersistentList()

private fun ThemeMode.title(): String = when (this) {
    ThemeMode.System -> "System"
    ThemeMode.Light -> "Light"
    ThemeMode.Dark -> "Dark"
}
