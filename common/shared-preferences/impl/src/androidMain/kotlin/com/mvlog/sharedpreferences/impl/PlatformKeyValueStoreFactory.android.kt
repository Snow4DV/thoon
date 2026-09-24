package com.mvlog.sharedpreferences.impl

import android.content.Context
import android.content.SharedPreferences
import com.mvlog.sharedpreferences.api.KeyValueStore
import com.mvlog.sharedpreferences.api.KeyValueStoreFactory

object AndroidPreferencesContext {

    private var applicationContext: Context? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun require(): Context = requireNotNull(applicationContext) {
        "AndroidPreferencesContext.install(context) must be called by rememberAppStartup " +
            "before a preference is read."
    }
}

internal actual fun platformKeyValueStoreFactory(): KeyValueStoreFactory =
    KeyValueStoreFactory { name ->
        SharedPreferencesKeyValueStore(
            AndroidPreferencesContext.require().getSharedPreferences(name, Context.MODE_PRIVATE),
        )
    }

private class SharedPreferencesKeyValueStore(
    private val preferences: SharedPreferences,
) : KeyValueStore {

    override fun getString(key: String): String? = preferences.getString(key, null)

    override fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}
