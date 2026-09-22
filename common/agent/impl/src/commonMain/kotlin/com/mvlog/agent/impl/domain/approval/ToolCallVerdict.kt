package com.mvlog.agent.impl.domain.approval

internal sealed interface ToolCallVerdict {

    /** Not gated, or an "always" rule matched. */
    data object Allowed : ToolCallVerdict

    data object Approved : ToolCallVerdict

    data object Declined : ToolCallVerdict

    data object Undecided : ToolCallVerdict
}

internal val ToolCallVerdict.executes: Boolean
    get() = this is ToolCallVerdict.Allowed || this is ToolCallVerdict.Approved

internal val ToolCallVerdict.isSettled: Boolean
    get() = this !is ToolCallVerdict.Undecided
