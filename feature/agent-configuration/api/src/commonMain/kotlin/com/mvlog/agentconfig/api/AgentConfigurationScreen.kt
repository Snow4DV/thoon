package com.mvlog.agentconfig.api

import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/** Null [section] is the root. */
@Serializable
data class AgentConfigurationScreen(val section: ConfigurationSection? = null) : Screen
