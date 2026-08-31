package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Immutable

/**
 * One row of a settings list.
 *
 * Each item carries its own behaviour rather than a key the screen looks up. A key-and-dispatch
 * design — `SettingChanged(key, value)` and a `when (key)` — would trade the compiler for strings in
 * exactly the place a settings screen accumulates them; here the item *is* the binding, so the UI is
 * a dumb renderer and a whole section is a plain function returning a list.
 *
 * That is what makes a section cheap: adding one costs a builder function, not a Screen, Presenter,
 * Ui, two factories and a registration.
 *
 * Growing this with a new *row type* is healthy. Growing it with conditional fields is the signal
 * that the thing being built is a form, not a settings list, and belongs on its own screen — which
 * is why Model & Provider has one.
 */
@Immutable
sealed interface SettingItem {

    /** Taps through to somewhere else. */
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

    /** One option of a single-select group; the group is just consecutive [Choice] rows. */
    data class Choice(
        val title: String,
        val subtitle: String? = null,
        val isSelected: Boolean,
        val onSelect: () -> Unit,
    ) : SettingItem

    /** Read-only. */
    data class Info(
        val title: String,
        val value: String,
    ) : SettingItem

    /**
     * Stands in for a section with nothing in it yet.
     *
     * Says *why* rather than merely that it is empty: a settings screen full of blank sections
     * otherwise reads as broken rather than unfinished.
     */
    data class Placeholder(val text: String) : SettingItem
}
