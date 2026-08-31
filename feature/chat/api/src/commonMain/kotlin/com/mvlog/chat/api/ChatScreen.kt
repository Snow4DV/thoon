package com.mvlog.chat.api

import com.mvlog.navigation.CommonParcelize
import com.slack.circuit.runtime.screen.Screen

/**
 * A single conversation.
 *
 * The whole of this module: what another feature needs to navigate here, and nothing about how the
 * screen is built. `ChatsListPresenter` calls `navigator.goTo(ChatScreen(id))` against this alone,
 * so it no longer compiles against chat's presenter, UI or DI.
 *
 * [chatId] is a plain `String` rather than the domain `ChatId`: a `Screen` must be `Parcelable` on
 * Android, and `@Parcelize` cannot handle value classes. The presenter factory wraps it — which also
 * keeps the domain type out of the navigation contract.
 *
 * Null means "start a new conversation"; the presenter creates one on first composition.
 *
 * [highlightMessageSequence] is `agent_chat_message.sequence` — set when the chat was opened from a
 * search result, so the screen can open at the message that matched instead of at the end. A `Long?`
 * for the same reason [chatId] is a `String`: `@Parcelize` cannot carry a value class.
 */
@CommonParcelize
data class ChatScreen(
    val chatId: String? = null,
    val highlightMessageSequence: Long? = null,
) : Screen
