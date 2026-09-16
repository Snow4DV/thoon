package com.mvlog.agent.impl.koog

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.mvlog.agent.impl.util.AgentClock

/** Named for the helper Koog 1.1.1 lacks; adopting theirs is a deletion. */
internal fun List<StreamFrame>.toMessageResponse(clock: AgentClock): Message.Assistant {
    val assembler = StreamFrameAssembler()
    forEach(assembler::accept)
    return Message.Assistant(
        parts = assembler.parts(),
        metaInfo = assembler.metaInfo() ?: ResponseMetaInfo(timestamp = clock.now()),
    )
}

/**
 * Tool calls and reasoning go back into the message too, or the model never sees its own prior
 * calls. Complete frames win over their deltas: providers may resend a whole section.
 */
internal class StreamFrameAssembler {

    private val text = StringBuilder()
    private var completedText: String? = null

    private val reasoningDeltas = mutableMapOf<String, StringBuilder>()
    private val completedReasoning = mutableMapOf<String, MessagePart.Reasoning>()

    private val toolCalls = mutableMapOf<String, MessagePart.Tool.Call>()

    private var metaInfo: ResponseMetaInfo? = null

    fun accept(frame: StreamFrame) {
        when (frame) {
            is StreamFrame.TextDelta -> text.append(frame.text)
            is StreamFrame.TextComplete -> completedText = frame.text

            is StreamFrame.ReasoningDelta -> frame.text?.let { delta ->
                reasoningDeltas.getOrPut(frame.id.orEmpty()) { StringBuilder() }.append(delta)
            }

            is StreamFrame.ReasoningComplete -> {
                val id = frame.id.orEmpty()
                reasoningDeltas.remove(id)
                completedReasoning[id] = MessagePart.Reasoning(
                    content = frame.content,
                    summary = frame.summary,
                    encrypted = frame.encrypted,
                    id = frame.id,
                )
            }

            is StreamFrame.ToolCallComplete -> toolCalls[frame.id ?: frame.name] =
                MessagePart.Tool.Call(
                    id = frame.id,
                    tool = frame.name,
                    args = frame.content,
                )

            is StreamFrame.End -> metaInfo = frame.metaInfo

            else -> Unit
        }
    }

    fun assistantText(): String = completedText ?: text.toString()

    fun metaInfo(): ResponseMetaInfo? = metaInfo

    /** Provider order: reasoning, text, tool calls. */
    fun parts(): List<MessagePart.ResponsePart> = buildList {
        completedReasoning.values.forEach(::add)
        reasoningDeltas.forEach { (id, builder) ->
            add(
                MessagePart.Reasoning(
                    content = listOf(builder.toString()),
                    summary = emptyList(),
                    encrypted = null,
                    id = id.ifEmpty { null },
                )
            )
        }

        assistantText().takeIf { it.isNotEmpty() }?.let { add(MessagePart.Text(it)) }

        toolCalls.values.forEach(::add)
    }
}
