package com.mvlog.agentconfig.presentation

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.PersistentList

/**
 * Every settings-shaped screen in this feature renders this.
 *
 * There is no event sink: each [SettingItem] carries its own behaviour, so there is nothing for the
 * UI to report back that the item cannot do itself.
 */
@Immutable
data class SettingsUiState(
    val title: String,
    val items: PersistentList<SettingItem>,
    val onBack: () -> Unit,
) : CircuitUiState
