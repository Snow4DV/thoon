package com.mvlog.agentconfig.presentation

import com.mvlog.navigation.CommonParcelize
import com.slack.circuit.runtime.screen.Screen

/**
 * The saved connection profiles.
 *
 * Not in the api module: nothing outside this feature navigates here directly. Reaching it goes
 * through [com.mvlog.agentconfig.api.AgentConfigurationScreen], which is the door the api publishes.
 */
@CommonParcelize
data object AgentConfigListScreen : Screen

/**
 * Create or edit one profile.
 *
 * [configId] null creates. A `String` rather than `AgentConfigId` because `@Parcelize` cannot carry
 * a value class.
 */
@CommonParcelize
data class AgentConfigEditorScreen(val configId: String? = null) : Screen
