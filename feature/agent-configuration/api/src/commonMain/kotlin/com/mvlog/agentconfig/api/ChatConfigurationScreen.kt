package com.mvlog.agentconfig.api

import com.mvlog.navigation.CommonParcelize
import com.slack.circuit.runtime.screen.Screen

/**
 * Which model one conversation uses.
 *
 * Separate from [AgentConfigurationScreen] because it carries an argument, and a
 * [ConfigurationSection] carries none.
 *
 * [chatId] is a plain `String` for the same reason `ChatScreen`'s is: a `Screen` must be
 * `Parcelable` on Android and `@Parcelize` cannot handle value classes.
 */
@CommonParcelize
data class ChatConfigurationScreen(val chatId: String) : Screen
