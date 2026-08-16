package com.mvlog.agent.impl.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Indirection so run timing and entry timestamps can be driven deterministically in tests. */
internal fun interface AgentClock {

    fun now(): Instant

    companion object {

        @OptIn(ExperimentalTime::class)
        val System: AgentClock = AgentClock { Clock.System.now() }
    }
}
