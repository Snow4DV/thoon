package com.mvlog.agenttools.web

/**
 * No parser on purpose. Script/style bodies are dropped, not just untagged, or minified JavaScript
 * fills the context window.
 */
internal object HtmlText {

    private val dropWholeElement = listOf("script", "style", "noscript", "svg", "head")

    fun extract(html: String): String {
        var text = html
        dropWholeElement.forEach { tag ->
            text = text.replace(
                Regex("<$tag\\b[^>]*>.*?</$tag>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
                " ",
            )
        }

        // Block closers become newlines so paragraphs and list items stay apart.
        text = text.replace(
            Regex("</(p|div|li|tr|h[1-6]|section|article|br)\\s*/?>", RegexOption.IGNORE_CASE),
            "\n",
        )
        text = text.replace(Regex("<[^>]+>"), " ")
        text = decodeEntities(text)

        return text.lines()
            .map { it.replace(Regex("[ \\t\\u00A0]+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    /** Only the handful that actually appear in prose; anything rarer reads fine as-is. */
    fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&#x27;", "'")
}
