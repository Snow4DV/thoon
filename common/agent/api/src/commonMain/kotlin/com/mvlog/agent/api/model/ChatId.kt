package com.mvlog.agent.api.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

/** Serializable because it travels inside a `Screen`, and the back stack is persisted. Encodes as the bare string. */
@Serializable
@JvmInline
value class ChatId(val value: String)
