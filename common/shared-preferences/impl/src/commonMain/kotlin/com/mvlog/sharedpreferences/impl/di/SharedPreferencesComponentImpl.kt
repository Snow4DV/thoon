package com.mvlog.sharedpreferences.impl.di

import com.mvlog.sharedpreferences.api.KeyValueStoreFactory
import com.mvlog.sharedpreferences.api.di.SharedPreferencesComponent

internal class SharedPreferencesComponentImpl(
    dependencies: SharedPreferencesComponentDependencies = SharedPreferencesComponentDependencies.Impl(),
    private val module: SharedPreferencesModule = SharedPreferencesModule.Impl(dependencies),
) : SharedPreferencesComponent {

    override fun keyValueStoreFactory(): KeyValueStoreFactory = module.keyValueStoreFactory
}
