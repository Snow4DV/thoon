package com.mvlog.chatslist.presentation.mapper

import com.mvlog.agent.api.model.ChatSearchResult
import com.mvlog.chatslist.presentation.ChatRow

/** How much of a chat id is enough to tell two rows apart without filling the line with a UUID. */
private const val ID_PREFIX_LENGTH = 8

/** A preview is one line in a list, so it is collapsed to one before it is measured. */
private const val SUBTITLE_LENGTH = 80

/**
 * Builds the row for a search result.
 *
 * Two fallbacks live here rather than in the UI, because both are decisions about what to say and
 * not about how to draw it:
 *
 * - A chat is named after its first prompt, so one abandoned before anything was asked has no title.
 *   Every such row would otherwise read the same.
 * - The subtitle prefers the text that matched over the last thing said. A result that cannot show
 *   why it matched is a filter wearing a search's clothes.
 */
internal fun ChatSearchResult.toRow(): ChatRow = ChatRow(
    id = chat.id.value,
    messageSequence = messageSequence,
    label = chat.title?.takeIf { it.isNotBlank() }
        ?: "Chat ${chat.id.value.take(ID_PREFIX_LENGTH)}",
    // The snippet arrives already windowed around the match, so it is not re-truncated here — that
    // is what used to cut the match off. The last-message fallback has no match to centre on.
    subtitle = snippet?.oneLine(collapseOnly = true) ?: chat.lastMessagePreview?.oneLine(),
)

private fun String.oneLine(collapseOnly: Boolean = false): String? {
    val collapsed = replace(Regex("\\s+"), " ").trim()
    return when {
        collapsed.isEmpty() -> null
        collapseOnly || collapsed.length <= SUBTITLE_LENGTH -> collapsed
        else -> collapsed.take(SUBTITLE_LENGTH).trimEnd() + "…"
    }
}
