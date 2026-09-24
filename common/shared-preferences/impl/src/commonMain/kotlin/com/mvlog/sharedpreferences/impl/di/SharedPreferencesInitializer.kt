package com.mvlog.sharedpreferences.impl.di

import com.mvlog.init.BaseInitializer
import com.mvlog.sharedpreferences.api.di.SharedPreferencesComponentHolder

class SharedPreferencesInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        SharedPreferencesComponentHolder.set { SharedPreferencesComponentImpl() }
    }

    private companion object {
        const val TAG = "SharedPreferences"
    }
}
