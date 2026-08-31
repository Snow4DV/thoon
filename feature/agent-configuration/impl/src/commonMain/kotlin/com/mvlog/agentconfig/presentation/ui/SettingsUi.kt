package com.mvlog.agentconfig.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.secondaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.agentconfig.presentation.SettingsUiState
import com.mvlog.agentconfig.presentation.ui.component.SettingRow
import com.mvlog.ui.components.ThoonTopBar

/**
 * Renders any settings-shaped screen in this feature.
 *
 * Knows nothing about what it is showing — the items arrived with their behaviour attached, so this
 * is the same code for the root, for a section, and for per-chat configuration.
 */
@Composable
fun SettingsUi(state: SettingsUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        ThoonTopBar(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme[colors][secondaryColor])
                .statusBarsPadding(),
            title = { Text(state.title, fontSize = 20.sp) },
            onBackClicked = state.onBack,
            onOptionsClick = null,
        )

        LazyColumn(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            items(state.items) { item -> SettingRow(item) }
        }
    }
}
