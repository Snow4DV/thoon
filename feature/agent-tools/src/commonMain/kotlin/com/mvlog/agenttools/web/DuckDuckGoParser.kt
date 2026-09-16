package com.mvlog.agenttools.web

/**
 * Scrapes the browser page — the only credential-free search; breaks silently when the markup
 * moves.
 */
internal object DuckDuckGoParser {

    private const val NO_RESULTS = "no-results"

    private val CHALLENGE_MARKERS = listOf("anomaly", "challenge")

    private val resultAnchor = Regex(
        """<a[^>]+class="[^"]*result__a[^"]*"[^>]*href="([^"]+)"[^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private val snippet = Regex(
        """<a[^>]+class="[^"]*result__snippet[^"]*"[^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /** The `no-results` container is what separates "nothing matched" from "markup moved". */
    fun isNoResultsPage(html: String): Boolean = NO_RESULTS in html

    fun isChallengePage(html: String): Boolean = CHALLENGE_MARKERS.any { it in html }

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

    /** Results are `/l/?uddg=<target>` redirects; the model needs the target to `fetch_url` it. */
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
