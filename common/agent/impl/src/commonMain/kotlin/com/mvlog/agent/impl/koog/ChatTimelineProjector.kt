package com.mvlog.agent.impl.koog

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.entity.ToolCallStatus
import kotlin.time.Instant

/** Timestamps missing from metadata are synthesised from sequence; the UI only needs order. */
internal class ChatTimelineProjector {

    fun project(chatId: ChatId, messages: List<Message>): List<ChatEntry> {
        val entries = mutableListOf<ChatEntry>()
        // A result lands in a later message than its call; calls are indexed so the result can
        // patch them.
        val toolCallsById = mutableMapOf<String, Int>()
        var sequence = 0L

        messages.forEachIndexed { messageIndex, message ->
            val timestamp = message.timestampOrNull() ?: Instant.fromEpochMilliseconds(sequence)

            when (message) {
                is Message.System -> Unit

                is Message.User -> {
                    message.parts.forEach { part ->
                        when (part) {
                            is MessagePart.Tool.Result -> {
                                val index = part.id?.let(toolCallsById::get) ?: return@forEach
                                val call = entries[index] as? ChatEntry.ToolCall ?: return@forEach
                                // Keep messageSequence: the entry anchors to the calling turn, not
                                // the result's.
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
                                // Running until a later result patches it; a call with no stored
                                // result never came back.
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
