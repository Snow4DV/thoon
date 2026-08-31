package com.mvlog.agentconfig.api

/**
 * The top-level groupings of the configuration screen.
 *
 * Here rather than in impl because it is a navigation argument. Only the identities are public —
 * the titles each one shows, and what it contains, belong to the feature that renders them.
 */
enum class ConfigurationSection {
    ModelAndProvider,
    Agent,
    Tools,
    Advanced,
}
