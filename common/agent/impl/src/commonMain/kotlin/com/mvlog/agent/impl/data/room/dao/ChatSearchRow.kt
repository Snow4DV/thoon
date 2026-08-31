package com.mvlog.agent.impl.data.room.dao

import androidx.room.Embedded
import com.mvlog.agent.impl.data.room.entity.ChatEntity

/**
 * One message that matched, and the chat it is in.
 *
 * A chat matching in three messages produces three of these. That is what lets a result open at the
 * message it is about rather than at the end of the conversation — [messageSequence] is
 * `agent_chat_message.sequence`, the anchor the timeline projector stamps onto every entry.
 *
 * [matchedText] is the whole matching text part, not a snippet: windowing it around the match needs
 * the query, which belongs above the database.
 */
data class ChatSearchRow(
    @Embedded val chat: ChatEntity,
    val messageSequence: Long,
    val matchedText: String,
    /**
     * Which text part of the turn matched.
     *
     * Selected rather than discarded because it is the `min()` that makes the bare columns beside
     * it well-defined — see the query.
     */
    val matchedPartSequence: Long,
)
