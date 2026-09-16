package com.mvlog.agent.api.model

data class ChatState(
    val chatId: ChatId,
    val items: List<ChatItem>,
    val execution: ChatExecutionState,
) {

    /** True while a run is queued or executing; render progress off this, details are in [execution]. */
    val isWorking: Boolean get() = execution is ChatExecutionState.Working
}
