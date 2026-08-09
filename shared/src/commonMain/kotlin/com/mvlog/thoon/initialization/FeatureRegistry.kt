package com.mvlog.thoon.initialization

import com.mvlog.init.BaseInitializer
import com.mvlog.log.TLogger

class FeatureRegistry {

    private val featureInitializers
        get() = listOf<BaseInitializer>(

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