package com.mvlog.sharedpreferences.impl

import com.mvlog.sharedpreferences.api.KeyValueStore
import com.mvlog.sharedpreferences.api.KeyValueStoreFactory
import platform.Foundation.NSUserDefaults

internal actual fun platformKeyValueStoreFactory(): KeyValueStoreFactory =
    KeyValueStoreFactory { name -> UserDefaultsKeyValueStore(prefix = "$name.") }

private class UserDefaultsKeyValueStore(private val prefix: String) : KeyValueStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getString(key: String): String? = defaults.stringForKey(prefix + key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = prefix + key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(prefix + key)
    }
}
