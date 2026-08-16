package com.mvlog.agent.impl.koog

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.mvlog.agent.impl.util.AgentClock

/**
 * Rebuilds the assistant reply a stream described.
 *
 * Named after the helper Koog is expected to ship eventually — it does not exist in 1.1.1, verified
 * against every artifact — so that adopting the library's version later is a deletion, not a
 * rewrite of the call site.
 */
internal fun List<StreamFrame>.toMessageResponse(clock: AgentClock): Message.Assistant {
    val assembler = StreamFrameAssembler()
    forEach(assembler::accept)
    return Message.Assistant(
        parts = assembler.parts(),
        metaInfo = assembler.metaInfo() ?: ResponseMetaInfo(timestamp = clock.now()),
    )
}

/**
 * Rebuilds an assistant reply from the frames that streamed it.
 *
 * Text, reasoning and tool calls are all carried back into the message, which is what lets the model
 * see its own prior tool usage on the next turn — accumulating text alone would silently drop it.
 *
 * Complete frames win over the deltas that preceded them: providers may resend a whole section, and
 * appending both would duplicate it.
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

    /**
     * Reasoning first, then text, then tool calls — the order providers use, and the order that
     * reads correctly when the parts are replayed.
     */
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
