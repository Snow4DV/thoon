package com.mvlog.settings.presentation.list

import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/**
 * The saved connection profiles.
 *
 * Not in the api module: nothing outside this feature navigates here directly. Reaching it goes
 * through [com.mvlog.settings.api.SettingsScreen], which is the door the api publishes.
 */
@Serializable
data object AgentConfigListScreen : Screen
