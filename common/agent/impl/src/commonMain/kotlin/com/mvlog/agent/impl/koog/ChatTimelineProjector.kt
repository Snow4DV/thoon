package com.mvlog.agent.impl.koog

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.entity.ToolCallStatus
import kotlin.time.Instant

/**
 * Turns a conversation back into the timeline the UI renders.
 *
 * This is what makes the durable state visible: reopening a chat replays the same messages the
 * model is replayed, so a run that died after making tool calls still shows them.
 *
 * The projection is lossy in one direction only — timestamps come from message metadata where it
 * exists and are otherwise synthesised, because ordering is what the UI needs and the conversation
 * is already in order.
 *
 * Every entry carries the index of the message it came from, and that index **is**
 * `agent_chat_message.sequence`: `KoogMessageRowCodec.toRows` writes `sequence = index` over the
 * whole list and `toMessages` sorts by it. That equality is what lets a search result — which knows
 * only a row — point at a rendered entry, whose own id is a counter over *visible* entries and
 * therefore not invertible. Anything that reorders or filters this input breaks the anchor.
 */
internal class ChatTimelineProjector {

    fun project(chatId: ChatId, messages: List<Message>): List<ChatEntry> {
        val entries = mutableListOf<ChatEntry>()
        // Results arrive in a later message than the call they answer, so calls are indexed as they
        // appear and revisited when their result shows up.
        val toolCallsById = mutableMapOf<String, Int>()
        var sequence = 0L

        messages.forEachIndexed { messageIndex, message ->
            val timestamp = message.timestampOrNull() ?: Instant.fromEpochMilliseconds(sequence)

            when (message) {
                // Never user-visible: it is instruction to the model, not part of the conversation.
                is Message.System -> Unit

                is Message.User -> {
                    message.parts.forEach { part ->
                        when (part) {
                            is MessagePart.Tool.Result -> {
                                val index = part.id?.let(toolCallsById::get) ?: return@forEach
                                val call = entries[index] as? ChatEntry.ToolCall ?: return@forEach
                                // `copy` deliberately leaves `messageSequence` alone: the entry
                                // belongs to the turn that *called* the tool, which is the one
                                // rendered. Restamping it here would anchor it to the result turn.
                                entries[index] = call.copy(
                                    status = ToolCallStatus.Completed(part.textOrEmpty()),
                                    updatedAt = timestamp,
                                )
                            }

                            else -> Unit
                        }
                    }

                    message.textContent().takeIf { it.isNotBlank() }?.let { text ->
                        entries += ChatEntry.UserMessage(
                            id = "${chatId.value}-$sequence",
                            chatId = chatId,
                            runId = null,
                            sequence = sequence++,
                            messageSequence = messageIndex.toLong(),
                            createdAt = timestamp,
                            updatedAt = timestamp,
                            text = text,
                        )
                    }
                }

                is Message.Assistant -> message.parts.forEach { part ->
                    when (part) {
                        is MessagePart.Reasoning -> entries += ChatEntry.Reasoning(
                            id = "${chatId.value}-$sequence",
                            chatId = chatId,
                            runId = null,
                            sequence = sequence++,
                            messageSequence = messageIndex.toLong(),
                            createdAt = timestamp,
                            updatedAt = timestamp,
                            text = (part.content + part.summary).joinToString("\n"),
                            isStreaming = false,
                        )

                        is MessagePart.Text -> entries += ChatEntry.AssistantMessage(
                            id = "${chatId.value}-$sequence",
                            chatId = chatId,
                            runId = null,
                            sequence = sequence++,
                            messageSequence = messageIndex.toLong(),
                            createdAt = timestamp,
                            updatedAt = timestamp,
                            text = part.text,
                            isStreaming = false,
                        )

                        is MessagePart.Tool.Call -> {
                            part.id?.let { toolCallsById[it] = entries.size }
                            entries += ChatEntry.ToolCall(
                                id = "${chatId.value}-$sequence",
                                chatId = chatId,
                                runId = null,
                                sequence = sequence++,
                                messageSequence = messageIndex.toLong(),
                                createdAt = timestamp,
                                updatedAt = timestamp,
                                toolCallId = part.id,
                                name = part.tool,
                                arguments = part.args,
                                // Nothing answered it in the durable record, so as far as anyone
                                // can tell the call never came back.
                                status = ToolCallStatus.Running,
                            )
                        }

                        else -> Unit
                    }
                }

                else -> Unit
            }
        }

        return entries
    }

    private fun Message.timestampOrNull(): Instant? = runCatching { metaInfo.timestamp }.getOrNull()

    private fun MessagePart.Tool.Result.textOrEmpty(): String =
        parts.filterIsInstance<MessagePart.Text>().joinToString("") { it.text }
}
