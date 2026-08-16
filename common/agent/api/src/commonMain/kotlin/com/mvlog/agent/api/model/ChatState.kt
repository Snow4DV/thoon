package com.mvlog.agent.api.model

data class ChatState(
    val chatId: ChatId,
    val items: List<ChatItem>,
    val execution: ChatExecutionState,
)
