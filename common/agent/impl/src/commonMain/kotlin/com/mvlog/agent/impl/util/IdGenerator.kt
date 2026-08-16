package com.mvlog.agent.impl.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun interface IdGenerator {

    fun newId(): String

    companion object {

        @OptIn(ExperimentalUuidApi::class)
        val Random: IdGenerator = IdGenerator { Uuid.random().toString() }
    }
}
