package com.mvlog.sharedpreferences.impl

import com.mvlog.sharedpreferences.api.KeyValueStoreFactory

internal expect fun platformKeyValueStoreFactory(): KeyValueStoreFactory
