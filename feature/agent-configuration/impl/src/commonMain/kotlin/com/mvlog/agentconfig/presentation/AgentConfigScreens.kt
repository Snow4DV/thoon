package com.mvlog.agentconfig.presentation

import com.mvlog.agent.api.model.AgentConfigId
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/**
 * The saved connection profiles.
 *
 * Not in the api module: nothing outside this feature navigates here directly. Reaching it goes
 * through [com.mvlog.agentconfig.api.AgentConfigurationScreen], which is the door the api publishes.
 */
@Serializable
data object AgentConfigListScreen : Screen

/**
 * Create or edit one profile. [configId] null creates.
 */
@Serializable
data class AgentConfigEditorScreen(val configId: AgentConfigId? = null) : Screen
