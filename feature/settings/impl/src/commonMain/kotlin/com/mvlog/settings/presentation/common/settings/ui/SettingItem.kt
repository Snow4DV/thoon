package com.mvlog.settings.presentation.common.settings.ui

import androidx.compose.runtime.Immutable

@Immutable
sealed interface SettingItem {

    data class Navigation(
        val title: String,
        val subtitle: String? = null,
        val onClick: () -> Unit,
    ) : SettingItem

    data class Toggle(
        val title: String,
        val subtitle: String? = null,
        val isOn: Boolean,
        val onChange: (Boolean) -> Unit,
    ) : SettingItem

    /** A single-select group is just consecutive [Choice] rows; there is no group object. */
    data class Choice(
        val title: String,
        val subtitle: String? = null,
        val isSelected: Boolean,
        val onSelect: () -> Unit,
    ) : SettingItem

    data class Info(
        val title: String,
        val value: String,
    ) : SettingItem

    /** [text] says why the section is empty, not merely that it is. */
    data class Placeholder(val text: String) : SettingItem
}
