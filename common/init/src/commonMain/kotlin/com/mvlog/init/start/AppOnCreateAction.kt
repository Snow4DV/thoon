package com.mvlog.init.start

class AppOnCreateAction(
    val tag: String, // not used right now - leave it - will be used for tracing later
    val action: () -> Unit,
)
