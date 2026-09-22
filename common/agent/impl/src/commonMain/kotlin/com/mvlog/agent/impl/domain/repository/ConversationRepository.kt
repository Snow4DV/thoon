package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.approval.PendingToolCall
import com.mvlog.agent.impl.domain.entity.ChatEntry

internal interface ConversationRepository {

    /**
     * Latest checkpoint if any, else committed history — the same rule
     * PersistentChatHistoryProvider restores the model with.
     */
    suspend fun timeline(chatId: ChatId): List<ChatEntry>

    suspend fun appendUserPrompt(chatId: ChatId, text: String)

    suspend fun unansweredPrompt(chatId: ChatId): String?

    suspend fun hasUnfinishedToolTurn(chatId: ChatId): Boolean

    /** The last turn's tool calls when it is an assistant turn nothing has answered; else empty. */
    suspend fun pendingToolCalls(chatId: ChatId): List<PendingToolCall>

    suspend fun chatsWithUnansweredPrompts(): List<ChatId>
}
