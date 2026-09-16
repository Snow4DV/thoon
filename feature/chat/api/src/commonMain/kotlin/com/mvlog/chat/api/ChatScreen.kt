package com.mvlog.chat.api

import com.mvlog.agent.api.model.ChatId
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/**
 * Null [chatId]: a new conversation; the chat screen creates it on first composition.
 * [highlightMessageSequence] is the search hit's `agent_chat_message.sequence`; the screen opens
 * there instead of at the end.
 */
@Serializable
data class ChatScreen(
    val chatId: ChatId? = null,
    val highlightMessageSequence: Long? = null,
) : Screen
