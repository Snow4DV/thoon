package com.mvlog.agentconfig.presentation.common.settings.ui

import androidx.compose.runtime.Immutable
import com.slack.circuit.runtime.CircuitUiState
import kotlinx.collections.immutable.PersistentList

/** No event sink: each [SettingItem] carries its own behaviour. */
@Immutable
data class SettingsUiState(
    val title: String,
    val items: PersistentList<SettingItem>,
    val onBack: () -> Unit,
) : CircuitUiState
