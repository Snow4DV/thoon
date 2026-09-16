package com.mvlog.log

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity

object TLogger {

    fun log(severity: Severity, tag: String, throwable: Throwable?, message: String) {
        if (Logger.config.minSeverity <= severity) {
            Logger.processLog(
                severity,
                tag,
                throwable,
                message,
            )
        }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) {
        Logger.d(message, throwable, tag)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        Logger.i(message, throwable, tag)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Logger.e(message, throwable, tag)
    }

    fun e(tag: String, throwable: Throwable) {
        Logger.e(throwable.message.orEmpty(), throwable, tag)
    }
}
