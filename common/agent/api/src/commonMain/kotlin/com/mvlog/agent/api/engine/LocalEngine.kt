package com.mvlog.agent.api.engine

/** A descriptor of what can be chosen, not a runnable engine. */
class LocalEngine(
    /** Persisted in configs; renaming orphans every config that selected it. */
    val id: String,
    val displayName: String,
)
