package com.mvlog.agentconfig.presentation

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agentconfig.presentation.mapper.describe
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

/**
 * The rows of a chat's configuration screen.
 *
 * Outside the presenter so the selection rule can be tested without a composition — and it is the
 * rule worth testing, because exactly one row must be selected. None, and the screen looks broken;
 * two, and it is lying about which model answers.
 *
 * [followsDefault] rather than a nullable override id: `observeChatConfig` reports the *effective*
 * configuration, so a chat with no override and a chat pinned to the same config as the default look
 * identical from here. Comparing resolved values is what keeps this honest when the default itself
 * changes underneath the screen.
 */
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

    // Last on purpose: a chat with no suitable model should be two taps from creating one rather
    // than a dead end.
    add(SettingItem.Navigation(title = "Manage models…", onClick = onManageModels))
}.toPersistentList()
