package com.mvlog.chatslist.presentation

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatSearchResult
import com.mvlog.agent.api.model.ChatSummary
import com.mvlog.chatslist.presentation.mapper.toRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ChatRowMapperTest {

    @Test
    fun aTitledChatUsesItsTitle() {
        assertEquals("Routes to Saint Petersburg", result(title = "Routes to Saint Petersburg").toRow().label)
    }

    @Test
    fun anUntitledChatFallsBackToItsId() {
        assertEquals("Chat 8424ee48", result(title = null).toRow().label)
    }

    @Test
    fun aBlankTitleIsTreatedAsNoTitle() {
        assertEquals("Chat 8424ee48", result(title = "   ").toRow().label)
    }

    @Test
    fun theMatchedTextWinsOverTheLastMessage() {
        val row = result(preview = "the most recent thing said", snippet = "the bit that matched").toRow()

        assertEquals("the bit that matched", row.subtitle)
    }

    @Test
    fun withoutAMatchTheLastMessageIsShown() {
        assertEquals("the most recent thing said", result(preview = "the most recent thing said").toRow().subtitle)
    }

    @Test
    fun aSubtitleIsCollapsedToOneLine() {
        val row = result(preview = "first line\n\n   second   line").toRow()

        assertEquals("first line second line", row.subtitle)
    }

    @Test
    fun aLongSubtitleIsTruncated() {
        val row = result(preview = "x".repeat(200)).toRow()

        assertTrue(row.subtitle!!.length < 100, "was ${row.subtitle!!.length}")
        assertTrue(row.subtitle!!.endsWith("…"))
    }

    @Test
    fun aChatThatHasSaidNothingHasNoSubtitle() {
        assertNull(result(preview = null).toRow().subtitle)
    }

    private fun result(
        title: String? = "Untitled",
        preview: String? = null,
        snippet: String? = null,
    ) = ChatSearchResult(
        messageSequence = null,
        chat = ChatSummary(
            id = ChatId("8424ee48-e993-4eba-950a-96d8b4f0f73d"),
            title = title,
            configId = AgentConfigId("config"),
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
            lastMessageAt = null,
            lastMessagePreview = preview,
        ),
        snippet = snippet,
    )
}
