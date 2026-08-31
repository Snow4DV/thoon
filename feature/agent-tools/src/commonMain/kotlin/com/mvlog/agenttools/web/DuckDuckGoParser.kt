package com.mvlog.agenttools.web

/**
 * Pulls results out of DuckDuckGo's HTML endpoint.
 *
 * This parses a page meant for a browser, not an API, because it is the only search that needs no
 * credential. It will break without warning the day the markup changes — which is precisely why
 * [parse] returning nothing must be treated as a failure by the caller rather than as "no results".
 * The two are indistinguishable from here, and only one of them is the user's problem.
 */
internal object DuckDuckGoParser {

    private val resultAnchor = Regex(
        """<a[^>]+class="[^"]*result__a[^"]*"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val snippet = Regex(
        """<a[^>]+class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun parse(html: String, limit: Int = 8): List<SearchResult> {
        val titles = resultAnchor.findAll(html).toList()
        val snippets = snippet.findAll(html).map { it.groupValues[1].asPlainText() }.toList()

        return titles.take(limit).mapIndexed { index, match ->
            SearchResult(
                title = match.groupValues[2].asPlainText(),
                url = match.groupValues[1].unwrapRedirect(),
                snippet = snippets.getOrNull(index).orEmpty(),
            )
        }
    }

    private fun String.asPlainText(): String =
        HtmlText.decodeEntities(replace(Regex("<[^>]+>"), ""))
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * DuckDuckGo wraps results in `/l/?uddg=<encoded target>`; the wrapper is useless to a model
     * that may want to fetch the page next.
     */
    private fun String.unwrapRedirect(): String {
        val marker = "uddg="
        val start = indexOf(marker)
        if (start < 0) return decodeUrlComponent(this)

        val value = substring(start + marker.length).substringBefore('&')
        return decodeUrlComponent(value)
    }

    private fun decodeUrlComponent(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            when {
                char == '%' && index + 2 < value.length -> {
                    val hex = value.substring(index + 1, index + 3).toIntOrNull(16)
                    if (hex != null) {
                        append(hex.toChar())
                        index += 3
                    } else {
                        append(char)
                        index++
                    }
                }

                char == '+' -> {
                    append(' ')
                    index++
                }

                else -> {
                    append(char)
                    index++
                }
            }
        }
    }
}

internal data class SearchResult(val title: String, val url: String, val snippet: String)
