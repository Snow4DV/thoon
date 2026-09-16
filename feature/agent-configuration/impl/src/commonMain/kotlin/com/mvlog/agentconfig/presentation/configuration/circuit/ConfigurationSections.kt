package com.mvlog.agentconfig.presentation.configuration.circuit

import com.mvlog.agentconfig.api.ConfigurationSection
import com.mvlog.agentconfig.presentation.common.settings.ui.SettingItem
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal fun ConfigurationSection.title(): String = when (this) {
    ConfigurationSection.ModelAndProvider -> "Model & Provider"
    ConfigurationSection.Agent -> "Agent"
    ConfigurationSection.Tools -> "Tools"
    ConfigurationSection.Advanced -> "Advanced"
}

internal fun ConfigurationSection.summary(): String = when (this) {
    ConfigurationSection.ModelAndProvider -> "Endpoints and models the agent can use"
    ConfigurationSection.Agent -> "How the agent behaves"
    ConfigurationSection.Tools -> "What the agent is allowed to do"
    ConfigurationSection.Advanced -> "Diagnostics and everything else"
}

/** From `entries`, so a new section appears here without being listed. */
internal fun configurationRootItems(
    onOpen: (ConfigurationSection) -> Unit,
): PersistentList<SettingItem> = ConfigurationSection.entries
    .map { section ->
        SettingItem.Navigation(
            title = section.title(),
            subtitle = section.summary(),
            onClick = { onOpen(section) },
        )
    }
    .toPersistentList()

internal fun configurationSectionItems(
    section: ConfigurationSection,
    onOpenModelAndProvider: () -> Unit,
): PersistentList<SettingItem> = when (section) {
    // The root row goes straight to the list; this branch is only reached via a restored back stack.
    ConfigurationSection.ModelAndProvider -> persistentListOf(
        SettingItem.Navigation(
            title = "Configurations",
            subtitle = "Endpoints and models the agent can use",
            onClick = onOpenModelAndProvider,
        ),
    )

    ConfigurationSection.Agent -> persistentListOf(
        SettingItem.Placeholder(
            "Nothing to configure yet. The system prompt is compiled in, and there is nowhere to " +
                "store an edited one — agent_settings holds only the default configuration.",
        ),
    )

    ConfigurationSection.Tools -> persistentListOf(
        SettingItem.Placeholder(
            "Nothing to configure yet. Tools are contributed by feature modules and are all on; " +
                "switching one off needs somewhere to remember that.",
        ),
    )

    ConfigurationSection.Advanced -> persistentListOf(
        SettingItem.Placeholder("Nothing to configure yet."),
    )
}
