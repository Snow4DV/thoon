package com.mvlog.sharedpreferences.impl.di

import com.mvlog.sharedpreferences.api.KeyValueStoreFactory
import com.mvlog.sharedpreferences.impl.platformKeyValueStoreFactory

internal interface SharedPreferencesComponentDependencies {

    val platformFactory: KeyValueStoreFactory
        get() = platformKeyValueStoreFactory()

    class Impl : SharedPreferencesComponentDependencies
}
