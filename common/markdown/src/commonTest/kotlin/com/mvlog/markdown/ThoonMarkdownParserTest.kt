package com.mvlog.markdown

import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.Paragraph
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pins the two parser behaviours the chat relies on; the composable itself is a thin call. */
class ThoonMarkdownParserTest {

    private val answer = """
        ## Route options

        The **fastest** one is the Sapsan.

        - Sapsan, 4 hours
        - Night train, 8 hours

        ```kotlin
        fun go() = println("Sapsan")
        ```
    """.trimIndent()

    @Test
    fun aTypicalAnswerParsesIntoTheExpectedBlocks() {
        val blocks = MarkdownParser().parse(answer).children.map { it::class.simpleName }

        assertEquals(
            listOf("Heading", "Paragraph", "ListBlock", "FencedCodeBlock"),
            blocks,
            "heading, prose, bullets and a fence are what answers are made of",
        )
    }

    @Test
    fun streamingInTokenSizedChunksMatchesASingleParse() {
        val streamed = MarkdownParser(appendCoalesceThreshold = 16).apply { beginStream() }
        answer.chunked(3).forEach { streamed.append(it) }
        val document = streamed.endStream()

        val whole = MarkdownParser().parse(answer)
        assertEquals(
            whole.children.map { it::class.simpleName },
            document.children.map { it::class.simpleName },
            "chunk boundaries inside tokens must not change the block structure",
        )
        assertEquals(
            (whole.children[3] as FencedCodeBlock).literal,
            (document.children[3] as FencedCodeBlock).literal,
            "the code block must arrive intact",
        )
        assertEquals(2, (document.children[0] as Heading).level)
        check(document.children[1] is Paragraph && document.children[2] is ListBlock)
    }
}
