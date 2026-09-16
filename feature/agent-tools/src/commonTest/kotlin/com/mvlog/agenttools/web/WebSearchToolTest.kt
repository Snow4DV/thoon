package com.mvlog.agenttools.web

import com.mvlog.agent.tool.ChatToolContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * What the tool *says* when it finds nothing.
 *
 * A real chat searched for `"meshersky bulvard"` — a misspelling the model had wrapped in quotes,
 * forcing an exact match that genuinely matched nothing. The tool reported it as "the provider's
 * markup has probably changed", which is a claim about the app rather than about the query. These
 * pin the three cases apart, because only one of them means anything is broken.
 */
class WebSearchToolTest {

    @Test
    fun anEmptyResultSetIsAnAnswerRatherThanAFailure() = runTest {
        val result = search(NO_RESULTS_PAGE)

        assertTrue("No results" in result, was(result))
        // The model's way out. Without it, the same exact-phrase query is all it can try.
        assertTrue("exact match" in result, was(result))
    }

    @Test
    fun anAntiBotChallengeIsReportedAsBlockedNotAsBroken() = runTest {
        // Served as 202, which is inside the 2xx range `isSuccess()` accepts — so before this was
        // handled it parsed to nothing and was blamed on the markup.
        val error = assertFailsWith<WebToolException> {
            search(CHALLENGE_PAGE, status = HttpStatusCode.Accepted)
        }

        assertTrue("blocked" in error.message.orEmpty(), was(error.message.orEmpty()))
        assertTrue("markup" !in error.message.orEmpty(), "being blocked is not a markup problem")
    }

    @Test
    fun anUnrecognisedPageIsStillReportedAsABrokenTool() = runTest {
        // The case the loud error was written for, and the only one that should still raise it:
        // no results, and no marker saying the provider meant it.
        val error = assertFailsWith<WebToolException> { search("<div>redesigned page</div>") }

        assertTrue("markup" in error.message.orEmpty(), was(error.message.orEmpty()))
    }

    @Test
    fun resultsAreReturnedWhenThePageHasThem() = runTest {
        val result = search(RESULTS_PAGE)

        assertTrue("Kotlin Multiplatform" in result, was(result))
        assertTrue("https://kotlinlang.org/multiplatform/" in result, was(result))
    }

    private suspend fun search(body: String, status: HttpStatusCode = HttpStatusCode.OK): String {
        val engine = MockEngine {
            respond(
                content = body,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }

        return WebSearchTool(HttpClient(engine)).execute(
            context = ChatToolContext(chatId = "chat"),
            arguments = JsonObject(mapOf("query" to JsonPrimitive("\"meshersky bulvard\""))),
        )
    }

    private fun was(value: String) = "was: $value"

    private companion object {
        /** The container class DuckDuckGo puts on a page it means to be empty. */
        const val NO_RESULTS_PAGE =
            """<div class="results"><div class="no-results">No results.</div></div>"""

        /** The 202 interstitial, which names its anomaly check. */
        const val CHALLENGE_PAGE =
            """<html><body><script src="/dist/anomaly.js"></script>challenge</body></html>"""

        const val RESULTS_PAGE = """
            <div class="results"><div class="result results_links">
              <a rel="nofollow" class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fkotlinlang.org%2Fmultiplatform%2F&amp;rut=e77">Kotlin Multiplatform</a>
              <a class="result__snippet" href="//duckduckgo.com/l/?uddg=x">A snippet.</a>
            </div></div>
        """
    }
}
