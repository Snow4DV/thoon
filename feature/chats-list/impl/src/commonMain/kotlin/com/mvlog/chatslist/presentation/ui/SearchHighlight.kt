package com.mvlog.chatslist.presentation.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * Case-insensitive, like the search itself.
 * Takes the [SpanStyle] so this stays a plain, composition-free function.
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
