package com.mvlog.chatslist.presentation.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * [text] with every occurrence of [query] styled.
 *
 * Marking the match is the difference between a result the reader can scan and one they have to
 * re-read: the snippet is already windowed around the match, but nothing in it says which words
 * were asked for.
 *
 * Case-insensitive, matching how the search itself matched. Takes a [SpanStyle] rather than reading
 * the theme so it stays a plain function — the colour resolves in composition, this does not.
 */
internal fun highlight(text: String, query: String, style: SpanStyle): AnnotatedString {
    val trimmed = query.trim()
    // An empty query would match at every index and never advance.
    if (trimmed.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        while (cursor <= text.length) {
            val match = text.indexOf(trimmed, startIndex = cursor, ignoreCase = true)
            if (match < 0) break

            append(text.substring(cursor, match))
            withStyle(style) { append(text.substring(match, match + trimmed.length)) }
            cursor = match + trimmed.length
        }
        append(text.substring(cursor))
    }
}
