package com.mvlog.agent.api.model

/**
 * One message that matched a search, and the chat it is in.
 *
 * A chat matching in three messages produces three of these. Showing them separately is what lets a
 * result be opened at the message it is about, rather than at the end of the conversation.
 *
 * [snippet] is the matching text windowed around the match, so the reason the row is here is on the
 * line. [messageSequence] identifies the turn. Both are null when the query was blank and every chat
 * is simply being listed.
 */
data class ChatSearchResult(
    val chat: ChatSummary,
    val snippet: String?,
    val messageSequence: Long?,
)
