package com.mvlog.agent.impl.koog

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.MessageMetaInfo
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import com.mvlog.agent.impl.domain.entity.StoredMessage
import com.mvlog.agent.impl.domain.entity.StoredPart
import com.mvlog.agent.impl.domain.entity.StoredPartKind
import com.mvlog.agent.impl.domain.entity.StoredRole
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.serialization.json.Json

internal class KoogMessageRowCodec(
    private val json: Json,
    private val idGenerator: IdGenerator,
) {

    fun toRows(messages: List<Message>): List<StoredMessage> =
        messages.mapIndexed { index, message ->
            StoredMessage(
                id = idGenerator.newId(),
                sequence = index.toLong(),
                role = message.role(),
                metaInfoJson = json.encodeToString(MessageMetaInfo.serializer(), message.metaInfo),
                parts = message.parts.mapIndexed { partIndex, part ->
                    part.toRow(sequence = partIndex.toLong())
                },
            )
        }

    /**
     * narrowTo throws rather than filters: a conversation silently missing a part is worse than one
     * that fails to load.
     */
    fun toMessages(rows: List<StoredMessage>): List<Message> =
        rows.sortedBy { it.sequence }.map { row ->
            val parts = row.parts.sortedBy { it.sequence }.map { it.toPart() }

            when (row.role) {
                StoredRole.System -> Message.System(
                    parts = parts.narrowTo<MessagePart.Text>(row),
                    metaInfo = row.metaInfo(),
                )

                StoredRole.User -> Message.User(
                    parts = parts.narrowTo<MessagePart.RequestPart>(row),
                    metaInfo = row.metaInfo(),
                )

                StoredRole.Assistant -> Message.Assistant(
                    parts = parts.narrowTo<MessagePart.ResponsePart>(row),
                    metaInfo = row.responseMetaInfo(),
                )
            }
        }

    private inline fun <reified T : MessagePart> List<MessagePart>.narrowTo(
        row: StoredMessage,
    ): List<T> = map { part ->
        part as? T ?: error(
            "Message ${row.id} is a ${row.role.stored} turn but holds a " +
                "${part::class.simpleName} part, which that role cannot carry.",
        )
    }

    private fun StoredMessage.metaInfo(): RequestMetaInfo =
        json.decodeFromString(MessageMetaInfo.serializer(), metaInfoJson) as RequestMetaInfo

    private fun StoredMessage.responseMetaInfo(): ResponseMetaInfo =
        json.decodeFromString(MessageMetaInfo.serializer(), metaInfoJson) as ResponseMetaInfo

    private fun Message.role(): StoredRole = when (this) {
        is Message.System -> StoredRole.System
        is Message.User -> StoredRole.User
        is Message.Assistant -> StoredRole.Assistant
        else -> error("Unsupported message type ${this::class.simpleName}")
    }

    private fun MessagePart.toRow(sequence: Long) = StoredPart(
        id = idGenerator.newId(),
        sequence = sequence,
        kind = kind(),
        text = (this as? MessagePart.Text)?.text,
        toolName = (this as? MessagePart.Tool)?.tool,
        payloadJson = json.encodeToString(MessagePart.serializer(), this),
    )

    private fun StoredPart.toPart(): MessagePart =
        json.decodeFromString(MessagePart.serializer(), payloadJson)

    private fun MessagePart.kind(): StoredPartKind = when (this) {
        is MessagePart.Text -> StoredPartKind.Text
        is MessagePart.Reasoning -> StoredPartKind.Reasoning
        is MessagePart.Tool.Call -> StoredPartKind.ToolCall
        is MessagePart.Tool.Result -> StoredPartKind.ToolResult
        is MessagePart.Attachment -> StoredPartKind.Attachment
        else -> error("Unsupported message part ${this::class.simpleName}")
    }
}
