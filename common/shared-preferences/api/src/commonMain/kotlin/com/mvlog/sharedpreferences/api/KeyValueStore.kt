package com.mvlog.sharedpreferences.api

/** Synchronous and plaintext: for small preferences read on the first frame, never secrets. */
interface KeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}

/** Stores created with different names never see each other's keys. */
fun interface KeyValueStoreFactory {
    fun create(name: String): KeyValueStore
}
