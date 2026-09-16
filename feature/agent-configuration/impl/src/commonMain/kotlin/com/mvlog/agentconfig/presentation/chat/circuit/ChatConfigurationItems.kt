package com.mvlog.agentconfig.presentation.chat.circuit

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agentconfig.presentation.common.settings.describe
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingItem
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/** Exactly one row is selected: the default row when [followsDefault], else the effective one. */
internal fun chatConfigurationItems(
    configs: List<AgentConfig>,
    effectiveId: String?,
    defaultName: String?,
    followsDefault: Boolean,
    onSelect: (AgentConfigId?) -> Unit,
    onManageModels: () -> Unit,
): PersistentList<SettingItem> = buildList {
    add(
        SettingItem.Choice(
            title = "Use the app default",
            subtitle = defaultName?.let { "Currently $it" } ?: "No default is set",
            isSelected = followsDefault,
            onSelect = { onSelect(null) },
        ),
    )

    configs.forEach { config ->
        add(
            SettingItem.Choice(
                title = config.name,
                subtitle = config.describe(),
                isSelected = !followsDefault && config.id.value == effectiveId,
                onSelect = { onSelect(config.id) },
            ),
        )
    }

    // Last on purpose: an empty registry must not be a dead end.
    add(SettingItem.Navigation(title = "Manage models…", onClick = onManageModels))
}.toPersistentList()
