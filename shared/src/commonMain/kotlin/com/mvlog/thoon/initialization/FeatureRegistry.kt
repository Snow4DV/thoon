package com.mvlog.thoon.initialization

import com.mvlog.agent.impl.di.ThoonAgentInitializer
import com.mvlog.thoon.database.DatabaseInitializer
import com.mvlog.init.BaseInitializer
import com.mvlog.log.TLogger

class FeatureRegistry {

    private val featureInitializers
        get() = listOf<BaseInitializer>(
            // Order-independent: every holder builds lazily on first access.
            DatabaseInitializer(),
            ThoonAgentInitializer(),
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