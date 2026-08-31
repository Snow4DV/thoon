package com.mvlog.agenttools.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Markup captured from the live endpoint, trimmed to two results.
 *
 * Real rather than invented, because every detail that breaks a naive parser is a detail this page
 * actually has: the href is a redirect wrapper, its query separator is HTML-encoded as `&amp;`, the
 * target is percent-encoded, and the snippet is peppered with `<b>` tags around the query terms.
 */
private val FIXTURE = """
<div class="results">
  <div class="result results_links">
    <a rel="nofollow" class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fkotlinlang.org%2Fmultiplatform%2F&amp;rut=e775348c3ad0986be4f39321209cbed5">Kotlin Multiplatform - Build Cross-Platform Apps</a>
    <a class="result__snippet" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fkotlinlang.org%2Fmultiplatform%2F&amp;rut=e775348c"><b>Kotlin</b> <b>Multiplatform</b> is a technology for reusing up to 100% of your code.</a>
  </div>
  <div class="result results_links">
    <a rel="nofollow" class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fdocs&amp;rut=abc">Docs &amp; guides</a>
    <a class="result__snippet" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fdocs&amp;rut=abc">Second snippet.</a>
  </div>
</div>
""".trimIndent()

class DuckDuckGoParserTest {

    @Test
    fun resultsAreReadOutOfTheLiveMarkup() {
        val results = DuckDuckGoParser.parse(FIXTURE)

        assertEquals(2, results.size)
        assertEquals("Kotlin Multiplatform - Build Cross-Platform Apps", results[0].title)
        assertTrue("technology for reusing" in results[0].snippet, was(results[0].snippet))
        assertTrue("<b>" !in results[0].snippet, "markup must not reach the model")
    }

    @Test
    fun theRedirectWrapperIsUnwrappedToTheRealUrl() {
        // The model may want to fetch_url one of these next, and DuckDuckGo's `/l/?uddg=` wrapper
        // is useless for that.
        val results = DuckDuckGoParser.parse(FIXTURE)

        assertEquals("https://kotlinlang.org/multiplatform/", results[0].url)
        assertEquals("https://example.com/docs", results[1].url)
    }

    @Test
    fun entitiesInTitlesAreDecoded() {
        assertEquals("Docs & guides", DuckDuckGoParser.parse(FIXTURE)[1].title)
    }

    @Test
    fun markupThatNoLongerMatchesYieldsNothing() {
        // The signal the tool turns into a loud failure. An empty parse is indistinguishable from
        // "no results" here, and only one of those is the user's problem — so the tool must never
        // report this as "nothing matched".
        assertTrue(DuckDuckGoParser.parse("<div>redesigned page</div>").isEmpty())
    }

    private fun was(value: String) = "was: $value"
}
