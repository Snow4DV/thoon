package com.mvlog.usersettings.impl.di

import com.mvlog.sharedpreferences.api.KeyValueStore
import com.mvlog.sharedpreferences.api.di.SharedPreferencesComponentHolder

internal interface UserSettingsComponentDependencies {

    val keyValueStore: KeyValueStore
        get() = SharedPreferencesComponentHolder.get().keyValueStoreFactory().create(STORE_NAME)

    class Impl : UserSettingsComponentDependencies

    private companion object {
        const val STORE_NAME = "thoon_user_settings"
    }
}
