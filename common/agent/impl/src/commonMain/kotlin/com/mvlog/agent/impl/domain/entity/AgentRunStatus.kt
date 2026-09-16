package com.mvlog.agent.impl.domain.entity

internal enum class AgentRunStatus {

    Queued,

    Running,

    Completed,

    Failed,

    Cancelled,

    Interrupted,
    ;

    val isPending: Boolean
        get() = this == Queued

    val isTerminal: Boolean
        get() = this == Completed || this == Failed || this == Cancelled || this == Interrupted
}
