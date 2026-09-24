package com.mvlog.sharedpreferences.impl

import com.mvlog.sharedpreferences.api.KeyValueStore
import com.mvlog.sharedpreferences.api.KeyValueStoreFactory

internal actual fun platformKeyValueStoreFactory(): KeyValueStoreFactory =
    KeyValueStoreFactory { name -> LocalStorageKeyValueStore(prefix = "$name.") }

private class LocalStorageKeyValueStore(private val prefix: String) : KeyValueStore {

    override fun getString(key: String): String? = localStorageGet(prefix + key)

    override fun putString(key: String, value: String) = localStorageSet(prefix + key, value)

    override fun remove(key: String) = localStorageRemove(prefix + key)
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun localStorageGet(key: String): String? = js("globalThis.localStorage.getItem(key)")

@OptIn(ExperimentalWasmJsInterop::class)
private fun localStorageSet(key: String, value: String): Unit =
    js("globalThis.localStorage.setItem(key, value)")

@OptIn(ExperimentalWasmJsInterop::class)
private fun localStorageRemove(key: String): Unit = js("globalThis.localStorage.removeItem(key)")
