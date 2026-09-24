package com.mvlog.thoon.initialization

import com.mvlog.agent.impl.di.ThoonAgentInitializer
import com.mvlog.settings.di.SettingsInitializer
import com.mvlog.agenttools.di.AgentToolsInitializer
import com.mvlog.chat.di.ChatInitializer
import com.mvlog.chatslist.di.ChatsListInitializer
import com.mvlog.thoon.database.DatabaseInitializer
import com.mvlog.init.BaseInitializer
import com.mvlog.log.TLogger
import com.mvlog.sharedpreferences.impl.di.SharedPreferencesInitializer
import com.mvlog.usersettings.impl.di.UserSettingsInitializer

class FeatureRegistry {

    private val featureInitializers
        get() = listOf<BaseInitializer>(
            DatabaseInitializer(),
            SharedPreferencesInitializer(),
            UserSettingsInitializer(),
            ThoonAgentInitializer(),
            AgentToolsInitializer(),
            SettingsInitializer(),
            ChatInitializer(),
            ChatsListInitializer(),
        )

    fun initialize() {
        featureInitializers.forEach { featureInitializer ->
            runCatching {
                featureInitializer.init()
            }.onFailure {
                TLogger.e(TAG, "Initializer '${featureInitializer.tag}' failed", it)
            }
        }
    }

    private companion object {
        const val TAG = "FeatureRegistry"
    }
}
