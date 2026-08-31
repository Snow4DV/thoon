package com.mvlog.chatslist.api

import com.mvlog.navigation.CommonParcelize
import com.slack.circuit.runtime.screen.Screen

/**
 * Every conversation, as a list.
 *
 * The whole of this module: what `App.kt` needs to name the root of the back stack, and nothing
 * about how the list is built.
 *
 * Takes no arguments, so it is a `data object` — unlike [com.mvlog.chat.api.ChatScreen], which
 * carries the chat it opens.
 */
@CommonParcelize
data object ChatsListScreen : Screen
