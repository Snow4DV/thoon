package com.mvlog.chatslist.presentation.mapper

import com.mvlog.agent.api.model.ChatSearchResult
import com.mvlog.chatslist.presentation.ChatRow

private const val ID_PREFIX_LENGTH = 8

private const val SUBTITLE_LENGTH = 80

internal fun ChatSearchResult.toRow(): ChatRow = ChatRow(
    id = chat.id.value,
    messageSequence = messageSequence,
    label = chat.title?.takeIf { it.isNotBlank() }
        ?: "Chat ${chat.id.value.take(ID_PREFIX_LENGTH)}",
    // Snippets arrive windowed around the match; re-truncating cut the match off.
    subtitle = snippet?.oneLine(collapseOnly = true) ?: chat.lastMessagePreview?.oneLine(),
    isWorking = chat.isWorking,
)

private fun String.oneLine(collapseOnly: Boolean = false): String? {
    val collapsed = replace(Regex("\\s+"), " ").trim()
    return when {
        collapsed.isEmpty() -> null
        collapseOnly || collapsed.length <= SUBTITLE_LENGTH -> collapsed
        else -> collapsed.take(SUBTITLE_LENGTH).trimEnd() + "…"
    }
}
