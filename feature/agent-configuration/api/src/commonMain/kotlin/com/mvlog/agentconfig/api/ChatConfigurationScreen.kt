package com.mvlog.agentconfig.api

import com.mvlog.agent.api.model.ChatId
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data class ChatConfigurationScreen(val chatId: ChatId) : Screen
