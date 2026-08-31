package com.mvlog.agent.impl.domain.entity

/** How much of a matching message to show around the match. */
private const val WINDOW_LENGTH = 80

/** How much of that window to spend on what came *before* the match, so it reads as context. */
private const val LEAD_LENGTH = 24

private val WHITESPACE = Regex("\\s+")

/**
 * The part of [text] worth showing for a search hit: the match, plus what surrounds it.
 *
 * A row is one line, and the interesting line is rarely the first one. Taking the head of a long
 * reply shows an opening sentence with no bearing on why the chat is in the results — the match can
 * be five hundred characters further down and never appear at all.
 *
 * Whitespace is collapsed *before* the match is located, not after: a fold applied to the text
 * afterwards would shift every offset computed against it.
 *
 * The match is located with `ignoreCase` against the original rather than by lowercasing both sides,
 * because `lowercase()` is not length-preserving — `İ` becomes two characters, and every offset past
 * it would be wrong. The cost is that char-by-char folding cannot reproduce every case the
 * Unicode-aware `lowercase()` behind `textLower` can, so the database can hand back a match this
 * cannot find; that falls back to the head of the message, which is what a result showed before.
 */
internal fun snippetAround(text: String, query: String): String {
    val collapsed = text.replace(WHITESPACE, " ").trim()
    val trimmedQuery = query.trim()

    val match = if (trimmedQuery.isEmpty()) -1 else collapsed.indexOf(trimmedQuery, ignoreCase = true)
    if (collapsed.length <= WINDOW_LENGTH) return collapsed
    if (match < 0) return collapsed.take(WINDOW_LENGTH).trimEnd() + "…"

    // Anchored so the whole match is inside even when it is longer than the window: the end is
    // measured from the match, not from the window, and the start is pulled back only as far as
    // there is text to pull back to.
    val start = minOf(maxOf(0, match - LEAD_LENGTH), collapsed.length - WINDOW_LENGTH)
    val end = minOf(collapsed.length, maxOf(start + WINDOW_LENGTH, match + trimmedQuery.length))

    val body = collapsed.substring(start, end)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < collapsed.length) "…" else ""
    return prefix + body.trim() + suffix
}

/**
 * Neutralises the wildcards in a `LIKE` pattern.
 *
 * Without this, searching for `50%` matches every message and `a_b` matches `axb`. The backslash is
 * escaped first, or escaping the others would double-escape it. Pairs with `ESCAPE '\'` in the
 * query — the two must be changed together.
 *
 * The escaped form is for SQL only. Highlighting matches against what the user actually typed.
 */
internal fun escapeLike(query: String): String = query
    .replace("\\", "\\\\")
    .replace("%", "\\%")
    .replace("_", "\\_")
