package com.mvlog.agentconfig.api

import com.mvlog.agent.api.model.ChatId
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/**
 * Which model one conversation uses.
 *
 * Separate from [AgentConfigurationScreen] because it carries an argument, and a
 * [ConfigurationSection] carries none.
 */
@Serializable
data class ChatConfigurationScreen(val chatId: ChatId) : Screen
