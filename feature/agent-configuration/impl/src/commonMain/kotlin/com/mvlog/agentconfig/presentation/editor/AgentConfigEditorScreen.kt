package com.mvlog.agentconfig.presentation.editor

import com.mvlog.agent.api.model.AgentConfigId
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/**
 * Create or edit one profile. [configId] null creates.
 *
 * Not in the api module: only the list navigates here.
 */
@Serializable
data class AgentConfigEditorScreen(val configId: AgentConfigId? = null) : Screen
