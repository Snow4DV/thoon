package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatEntry

/**
 * The durable conversation, in domain terms.
 *
 * Sits above committed history and checkpoints and answers the two questions the rest of the module
 * actually has: what should the user see, and is there a prompt still waiting for an answer.
 * Neither caller needs to know a conversation is stored as agent-framework messages.
 */
internal interface ConversationRepository {

    /**
     * The conversation as timeline entries — uncommitted work if a run left any, else committed
     * history. The same rule the model is restored with, so the two never disagree.
     */
    suspend fun timeline(chatId: ChatId): List<ChatEntry>

    /**
     * Records an accepted prompt before anything runs it.
     *
     * Durability starts here rather than when a run begins: a prompt is either answered or visibly
     * unanswered, and never simply lost to a process that died in between.
     */
    suspend fun appendUserPrompt(chatId: ChatId, text: String)

    /** The prompt awaiting an answer in this chat, if its conversation ends with one. */
    suspend fun unansweredPrompt(chatId: ChatId): String?

    /** Every chat holding a prompt that was accepted but never answered. */
    suspend fun chatsWithUnansweredPrompts(): List<ChatId>
}
