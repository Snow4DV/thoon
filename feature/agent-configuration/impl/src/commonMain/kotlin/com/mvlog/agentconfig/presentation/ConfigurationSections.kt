package com.mvlog.agentconfig.presentation

import com.mvlog.agentconfig.api.ConfigurationSection
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

/**
 * What each part of the settings tree contains.
 *
 * Plain functions rather than presenter methods so they can be read and tested without a
 * composition — the same reason `ChatRowMapper` sits outside its presenter.
 */

/** Titles live here, not in the api module, which stays a navigation key and nothing more. */
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

/**
 * The root: one row per section, in declaration order.
 *
 * Built from `entries` rather than listed by hand, so a section added to the enum appears here
 * without anyone remembering to add it.
 */
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

/**
 * One section's contents.
 *
 * The `when` is exhaustive, so a section added to [ConfigurationSection] fails to compile until it
 * has something to show — which is a better guarantee than a test.
 *
 * Three of the four are empty, and say why: `agent_settings` holds only the default configuration,
 * so there is nowhere to put an edited system prompt or a switched-off tool. A blank section with no
 * explanation reads as broken rather than unfinished.
 */
internal fun configurationSectionItems(
    section: ConfigurationSection,
    onOpenModelAndProvider: () -> Unit,
): PersistentList<SettingItem> = when (section) {
    // Not a settings list — it is master-detail CRUD, so it has its own screen. Reached directly
    // from the root; this branch only matters if the section is navigated to some other way, such
    // as a restored back stack.
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
