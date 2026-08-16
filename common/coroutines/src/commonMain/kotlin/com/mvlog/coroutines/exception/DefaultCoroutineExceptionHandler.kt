package com.mvlog.coroutines.exception

import com.mvlog.log.TLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

internal class DefaultCoroutineExceptionHandler(
    private val tag: String,
) : CoroutineExceptionHandler {

    override fun handleException(
        context: CoroutineContext,
        exception: Throwable
    ) {
        TLogger.e(tag, "Coroutine threw an exception", exception)
    }

    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler
}
