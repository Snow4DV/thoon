package com.mvlog.agentconfig.api

import com.mvlog.navigation.CommonParcelize
import com.slack.circuit.runtime.screen.Screen

/**
 * The settings screen.
 *
 * One screen for the whole tree rather than one per section: a section is a list of rows, the root
 * is a list of rows whose taps navigate, and both render identically. A new section is an entry in
 * [ConfigurationSection] and a builder function, not another Screen, Presenter, Ui and registration.
 *
 * [section] null is the root.
 */
@CommonParcelize
data class AgentConfigurationScreen(val section: ConfigurationSection? = null) : Screen
