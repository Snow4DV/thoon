package com.mvlog.agenttools.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HtmlTextTest {

    @Test
    fun scriptAndStyleBodiesAreRemovedEntirely() {
        val text = HtmlText.extract(
            """
            <html><head><style>.a{color:red}</style></head>
            <body><script>var x = 1;</script><p>Real content</p></body></html>
            """.trimIndent()
        )

        assertEquals("Real content", text)
    }

    @Test
    fun blockElementsBecomeLineBreaks() {
        val text = HtmlText.extract("<p>One</p><p>Two</p><li>Three</li>")

        assertEquals(listOf("One", "Two", "Three"), text.lines())
    }

    @Test
    fun entitiesAreDecodedAndWhitespaceCollapses() {
        val text = HtmlText.extract("<p>a &amp;   b&nbsp;&nbsp;c</p>")

        assertEquals("a & b c", text)
    }

    @Test
    fun aScriptRenderedPageYieldsNothingRatherThanTagSoup() {
        val text = HtmlText.extract("<html><body><div id=\"root\"></div></body></html>")

        assertTrue(text.isBlank(), "the tool turns this into a stated failure, was: '$text'")
    }
}
