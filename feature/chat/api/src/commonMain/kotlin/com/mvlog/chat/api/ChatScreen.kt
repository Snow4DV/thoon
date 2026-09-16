package com.mvlog.chat.api

import com.mvlog.agent.api.model.ChatId
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/**
 * A single conversation.
 *
 * The whole of this module: what another feature needs to navigate here, and nothing about how the
 * screen is built. `ChatsListPresenter` calls `navigator.goTo(ChatScreen(id))` against this alone,
 * so it no longer compiles against chat's presenter, UI or DI.
 *
 * Null [chatId] means "start a new conversation"; the presenter creates one on first composition.
 *
 * [highlightMessageSequence] is `agent_chat_message.sequence` — set when the chat was opened from a
 * search result, so the screen can open at the message that matched instead of at the end.
 *
 * `@Serializable` because the back stack is persisted through kotlinx-serialization; the serializer
 * is registered next to the screen factory in `ChatInitializer`.
 */
@Serializable
data class ChatScreen(
    val chatId: ChatId? = null,
    val highlightMessageSequence: Long? = null,
) : Screen
