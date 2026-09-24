package com.mvlog.coroutines.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// The browser has one thread and no blocking I/O to offload.
internal actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
