package com.mvlog.agent.api.engine

/**
 * An on-device inference engine that a configuration may select.
 *
 * A descriptor, not an implementation: it says what can be *chosen*. Running it is separate, and a
 * real engine must also teach the client factory how to serve [com.mvlog.agent.api.model.AgentConfig.Local]
 * — registering a descriptor alone leaves every run failing.
 */
class LocalEngine(
    /** Stored in the configuration, so renaming one orphans every config that selected it. */
    val id: String,
    val displayName: String,
)
