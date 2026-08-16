package com.mvlog.coroutines.scope

import com.mvlog.coroutines.exception.DefaultCoroutineExceptionHandler
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

object ProcessScope : CoroutineScope {
    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default + createUncaughtExceptionsHandler()

    private fun createUncaughtExceptionsHandler(): CoroutineExceptionHandler {
        return DefaultCoroutineExceptionHandler("ProcessScope")
    }
}
