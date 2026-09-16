package com.mvlog.agentconfig.presentation.common.settings.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.ui.components.Icon
import com.composables.ui.components.Switch
import com.composables.ui.components.Text
import com.composables.ui.theme.colors
import com.composables.ui.theme.mutedColor
import com.composables.ui.theme.primaryColor
import com.composeunstyled.theme.Theme
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingItem
import com.mvlog.ui.ThoonTypography

@Composable
internal fun SettingRow(item: SettingItem, modifier: Modifier = Modifier) {
    when (item) {
        is SettingItem.Navigation -> RowShell(
            title = item.title,
            subtitle = item.subtitle,
            onClick = item.onClick,
            modifier = modifier,
            trailing = {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    tint = Theme[colors][mutedColor],
                    modifier = Modifier.size(18.dp),
                )
            },
        )

        is SettingItem.Toggle -> RowShell(
            title = item.title,
            subtitle = item.subtitle,
            onClick = { item.onChange(!item.isOn) },
            modifier = modifier,
            trailing = {
                Switch(checked = item.isOn, onCheckedChange = item.onChange)
            },
        )

        is SettingItem.Choice -> RowShell(
            title = item.title,
            subtitle = item.subtitle,
            onClick = item.onSelect,
            modifier = modifier,
            trailing = {
                // Only the selected row draws, so the column reads as one answer.
                if (item.isSelected) {
                    Icon(
                        imageVector = Lucide.Check,
                        tint = Theme[colors][primaryColor],
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
        )

        is SettingItem.Info -> RowShell(
            title = item.title,
            subtitle = item.value,
            onClick = null,
            modifier = modifier,
            trailing = {},
        )

        is SettingItem.Placeholder -> Text(
            text = item.text,
            style = ThoonTypography.body,
            color = Theme[colors][mutedColor],
            modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
        )
    }
}

@Composable
private fun RowShell(
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = ThoonTypography.body)
            subtitle?.let {
                Text(text = it, style = ThoonTypography.caption, color = Theme[colors][mutedColor])
            }
        }
        trailing()
    }
}
