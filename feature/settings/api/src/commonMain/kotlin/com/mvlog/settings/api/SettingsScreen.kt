package com.mvlog.settings.api

import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

/** Null [section] is the root. */
@Serializable
data class SettingsScreen(val section: SettingsSection? = null) : Screen
