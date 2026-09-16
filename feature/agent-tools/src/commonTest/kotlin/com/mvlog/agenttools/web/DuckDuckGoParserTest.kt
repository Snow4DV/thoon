package com.mvlog.agenttools.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Captured from the live endpoint: the redirect wrapper, `&amp;` separator, percent-encoded target
 * and `<b>` tags are all real.
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
        assertTrue(DuckDuckGoParser.parse("<div>redesigned page</div>").isEmpty())
    }

    @Test
    fun apageThatMeansItFoundNothingSaysSo() {
        assertTrue(DuckDuckGoParser.isNoResultsPage("""<div class="no-results">No results.</div>"""))
        assertTrue(!DuckDuckGoParser.isNoResultsPage(FIXTURE))
    }

    @Test
    fun theAntiBotInterstitialIsRecognised() {
        assertTrue(DuckDuckGoParser.isChallengePage("""<script src="/dist/anomaly.js"></script>"""))
        assertTrue(!DuckDuckGoParser.isChallengePage(FIXTURE))
    }

    private fun was(value: String) = "was: $value"
}
