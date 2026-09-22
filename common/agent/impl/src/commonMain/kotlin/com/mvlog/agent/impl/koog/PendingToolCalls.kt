package com.mvlog.agent.impl.koog

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import com.mvlog.agent.impl.domain.approval.PendingToolCall
import com.mvlog.agent.impl.domain.approval.toolCallKey

internal fun List<MessagePart.Tool.Call>.toPendingToolCalls(): List<PendingToolCall> =
    mapIndexed { ordinal, part ->
        PendingToolCall(
            key = toolCallKey(part.id, ordinal),
            id = part.id,
            name = part.tool,
            argumentsJson = part.args,
        )
    }

internal fun Message?.trailingToolCalls(): List<MessagePart.Tool.Call> =
    (this as? Message.Assistant)?.parts?.filterIsInstance<MessagePart.Tool.Call>().orEmpty()
