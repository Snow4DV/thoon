package com.mvlog.agent.impl.domain.entity

internal enum class AgentRunStatus {

    /** Accepted and durable, not picked up by the coordinator yet. */
    Queued,

    Running,

    Completed,

    Failed,

    Cancelled,

    /**
     * Was [Running] when the process died. Distinct from [Failed] so recovery can treat it
     * differently once checkpointing exists.
     */
    Interrupted,
    ;

    val isPending: Boolean
        get() = this == Queued

    val isTerminal: Boolean
        get() = this == Completed || this == Failed || this == Cancelled || this == Interrupted
}
