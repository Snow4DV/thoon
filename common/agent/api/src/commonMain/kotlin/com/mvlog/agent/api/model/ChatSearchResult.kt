package com.mvlog.agent.api.model

/**
 * One matching message and its chat — three matches in one chat are three results. [snippet] and
 * [messageSequence] are null when the query was blank and every chat is listed.
 */
data class ChatSearchResult(
    val chat: ChatSummary,
    val snippet: String?,
    val messageSequence: Long?,
)
