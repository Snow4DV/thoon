package com.mvlog.agentconfig.di

import com.mvlog.navigation.screen.ScreenFactory

internal interface AgentConfigurationComponent {

    fun screenFactory(): ScreenFactory
}
