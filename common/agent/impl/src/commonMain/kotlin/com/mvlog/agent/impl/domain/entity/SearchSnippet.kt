package com.mvlog.agent.impl.domain.entity

private const val WINDOW_LENGTH = 80

/** Context before the match; the rest of the window follows it. */
private const val LEAD_LENGTH = 24

private val WHITESPACE = Regex("\\s+")

internal fun snippetAround(text: String, query: String): String {
    // Collapse before locating the match: folding afterwards shifts every offset.
    val collapsed = text.replace(WHITESPACE, " ").trim()
    val trimmedQuery = query.trim()

    // indexOf(ignoreCase) rather than lowercase(): lowercase is not length-preserving
    // (İ → 2 chars). A match SQL found that this cannot falls back to the head.
    val match = if (trimmedQuery.isEmpty()) -1 else collapsed.indexOf(trimmedQuery, ignoreCase = true)
    if (collapsed.length <= WINDOW_LENGTH) return collapsed
    if (match < 0) return collapsed.take(WINDOW_LENGTH).trimEnd() + "…"

    // End is measured from the match, not the window, so a match longer than the window is never
    // clipped.
    val start = minOf(maxOf(0, match - LEAD_LENGTH), collapsed.length - WINDOW_LENGTH)
    val end = minOf(collapsed.length, maxOf(start + WINDOW_LENGTH, match + trimmedQuery.length))

    val body = collapsed.substring(start, end)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < collapsed.length) "…" else ""
    return prefix + body.trim() + suffix
}

/**
 * Pairs with ESCAPE '\' in ChatDao.search — change both together. Backslash first or the rest
 * double-escape. SQL only; highlighting uses the raw query.
 */
internal fun escapeLike(query: String): String = query
    .replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")
