package com.mvlog.settings.presentation.settings.circuit

import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import com.mvlog.settings.api.SettingsSection
import com.mvlog.settings.presentation.common.settings.themeModeItems
import com.mvlog.settings.presentation.common.settings.toolApprovalItems
import com.mvlog.settings.presentation.common.settings.ui.SettingItem
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import com.mvlog.usersettings.api.model.ThemeMode
import kotlinx.collections.immutable.toPersistentList

internal fun SettingsSection.title(): String = when (this) {
    SettingsSection.ModelAndProvider -> "Model & Provider"
    SettingsSection.Agent -> "Agent"
    SettingsSection.Tools -> "Tools"
    SettingsSection.Appearance -> "Appearance"
    SettingsSection.Advanced -> "Advanced"
}

internal fun SettingsSection.summary(): String = when (this) {
    SettingsSection.ModelAndProvider -> "Endpoints and models the agent can use"
    SettingsSection.Agent -> "How the agent behaves"
    SettingsSection.Tools -> "What the agent is allowed to do"
    SettingsSection.Appearance -> "Light, dark or follow the system"
    SettingsSection.Advanced -> "Diagnostics and everything else"
}

/** From `entries`, so a new section appears here without being listed. */
internal fun settingsRootItems(
    onOpen: (SettingsSection) -> Unit,
): PersistentList<SettingItem> = SettingsSection.entries
    .map { section ->
        SettingItem.Navigation(
            title = section.title(),
            subtitle = section.summary(),
            onClick = { onOpen(section) },
        )
    }
    .toPersistentList()

internal fun settingsSectionItems(
    section: SettingsSection,
    onOpenModelAndProvider: () -> Unit,
    gatedTools: List<String> = emptyList(),
    approvalRules: List<ToolApprovalRule> = emptyList(),
    onAllowTool: (String) -> Unit = {},
    onRevokeRule: (ToolApprovalRuleId) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.System,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
): PersistentList<SettingItem> = when (section) {
    // The root row goes straight to the list; this branch is only reached via a restored back stack.
    SettingsSection.ModelAndProvider -> persistentListOf(
        SettingItem.Navigation(
            title = "Configurations",
            subtitle = "Endpoints and models the agent can use",
            onClick = onOpenModelAndProvider,
        ),
    )

    SettingsSection.Agent -> persistentListOf(
        SettingItem.Placeholder(
            "Nothing to configure yet. The system prompt is compiled in, and there is nowhere to " +
                "store an edited one — agent_settings holds only the default configuration.",
        ),
    )

    SettingsSection.Tools -> toolApprovalItems(
        gatedTools = gatedTools,
        rules = approvalRules,
        scopeSubtitle = "Everywhere",
        onAllow = onAllowTool,
        onRevoke = onRevokeRule,
    ).ifEmpty {
        listOf(
            SettingItem.Placeholder(
                "No tool asks for approval, so there is nothing to allow in advance. Switching a " +
                    "tool off entirely still needs somewhere to remember that.",
            ),
        )
    }.toPersistentList()

    SettingsSection.Appearance -> themeModeItems(
        selected = themeMode,
        onSelect = onSelectThemeMode,
    )

    SettingsSection.Advanced -> persistentListOf(
        SettingItem.Placeholder("Nothing to configure yet."),
    )
}
