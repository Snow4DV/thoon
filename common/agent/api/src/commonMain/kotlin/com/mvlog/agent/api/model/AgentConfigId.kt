package com.mvlog.agent.api.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class AgentConfigId(val value: String)
