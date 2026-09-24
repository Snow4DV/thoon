package com.mvlog.usersettings.impl.di

import com.mvlog.init.BaseInitializer
import com.mvlog.usersettings.api.di.UserSettingsComponentHolder

class UserSettingsInitializer : BaseInitializer(tag = TAG) {

    override fun init() {
        UserSettingsComponentHolder.set { UserSettingsComponentImpl() }
    }

    private companion object {
        const val TAG = "UserSettings"
    }
}
