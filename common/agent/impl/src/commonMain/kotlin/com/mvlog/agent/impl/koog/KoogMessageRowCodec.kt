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

/**
 * The only place in the module that knows what a Koog [Message] is on the storage path.
 *
 * Storage holds turns and their parts as rows so that a conversation can be queried — which is what
 * makes searching messages a `WHERE` clause instead of decoding every conversation in the app. This
 * translates between that shape and the framework's.
 *
 * **Fidelity is not this file's invention.** Each part is stored as its own serialised
 * [MessagePart], so anything the row model does not name — encrypted reasoning, attachment sources,
 * cache control — round-trips through Koog's own serialiser. Only grouping and ordering are ours,
 * and those are what the round-trip test pins.
 */
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
     * Rebuilds the conversation the framework was given.
     *
     * Parts are narrowed by the role that holds them — `Message.User` accepts only request parts and
     * `Message.Assistant` only response parts — so a part stored under a role that cannot carry it
     * is a real inconsistency. It throws rather than filtering, because a conversation quietly
     * missing a tool call is worse than one that fails to load.
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
        // Only text carries a searchable body. Reasoning and tool output are stored in full but
        // left out of this column, which is what stops a search matching what the model merely
        // thought or what a fetched page happened to say.
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
