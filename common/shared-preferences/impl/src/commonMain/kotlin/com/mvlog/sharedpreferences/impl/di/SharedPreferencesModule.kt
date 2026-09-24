package com.mvlog.sharedpreferences.impl.di

import com.mvlog.sharedpreferences.api.KeyValueStoreFactory

internal interface SharedPreferencesModule {
    val keyValueStoreFactory: KeyValueStoreFactory

    class Impl(dependencies: SharedPreferencesComponentDependencies) :
        SharedPreferencesModule, SharedPreferencesComponentDependencies by dependencies {

        override val keyValueStoreFactory: KeyValueStoreFactory
            get() = platformFactory
    }
}
