package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId
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

    suspend fun chatsWithUnansweredPrompts(): List<ChatId>
}
