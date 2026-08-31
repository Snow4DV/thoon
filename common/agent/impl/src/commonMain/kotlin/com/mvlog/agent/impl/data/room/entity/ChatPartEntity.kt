package com.mvlog.agent.impl.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One piece of content inside a turn.
 *
 * Separate rows rather than a blob on the message, because what a human said and what the model
 * privately thought sit *side by side in the same turn* — and only one of them should ever be
 * searched. As rows they are told apart by [kind], so a search that filters on it cannot match
 * reasoning by accident. Flattened into one text field they could not be told apart at all.
 *
 * [payloadJson] is the exact serialised framework part, which is what makes this lossless: fields
 * this table does not model — encrypted reasoning, attachment sources, cache control — round-trip
 * through the framework's own serialiser rather than through a mapping written here.
 */
@Entity(
    tableName = "agent_chat_part",
    indices = [Index("messageId", "sequence"), Index("kind")],
    foreignKeys = [
        ForeignKey(
            entity = ChatMessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["messageId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ChatPartEntity(
    @PrimaryKey
    val id: String,
    val messageId: String,
    /** Position within the turn. Reasoning precedes the answer it produced; order is meaning. */
    val sequence: Long,
    /** `text`, `reasoning`, `tool_call`, `tool_result` or `attachment`. */
    val kind: String,
    /** Populated for text parts only. Null elsewhere, which is what keeps search honest. */
    val text: String?,
    /**
     * [text] lowercased in Kotlin.
     *
     * Not `lower(text)` in SQL: SQLite folds case for ASCII only, so `LIKE '%привет%'` would never
     * match `Привет`. Kotlin's `lowercase()` is Unicode-aware, so the folding happens on write.
     */
    val textLower: String?,
    /** Populated for tool calls, so asking which tools a chat used is a query rather than a parse. */
    val toolName: String?,
    val payloadJson: String,
)
