package com.mvlog.sharedpreferences.api.di

import com.mvlog.sharedpreferences.api.KeyValueStoreFactory

interface SharedPreferencesComponent {
    fun keyValueStoreFactory(): KeyValueStoreFactory
}
