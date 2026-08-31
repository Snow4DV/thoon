package com.mvlog.agent.impl

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.entity.ToolCallStatus
import com.mvlog.agent.impl.koog.ChatTimelineProjector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * The anchor a search result points at.
 *
 * An entry's own id is a counter over *rendered* entries, and that counter cannot be inverted: a
 * system turn is skipped, a user turn collapses into one entry, an assistant turn expands into one
 * per part. `messageSequence` is the index of the source message instead, which equals
 * `agent_chat_message.sequence` because `KoogMessageRowCodec` writes and reads it as the list index.
 *
 * Every deep link from search rests on that equality, and nothing else pins it.
 */
class ChatTimelineProjectorTest {

    @Test
    fun everyEntryCarriesTheIndexOfItsSourceMessage() {
        val entries = ChatTimelineProjector().project(CHAT, conversation())

        // Message 0 is the system prompt and renders nothing; the first visible entry is message 1.
        val user = entries.filterIsInstance<ChatEntry.UserMessage>().first()
        assertEquals(1L, user.messageSequence, "the prompt is the second stored turn")

        val assistant = entries.filterIsInstance<ChatEntry.AssistantMessage>().first()
        val reasoning = entries.filterIsInstance<ChatEntry.Reasoning>().first()
        assertEquals(2L, assistant.messageSequence)
        assertEquals(
            2L,
            reasoning.messageSequence,
            "reasoning and the reply it produced are one turn, so they share one anchor",
        )
    }

    @Test
    fun askippedSystemTurnDoesNotShiftTheAnchor() {
        // The rendered counter and the stored sequence disagree from the very first entry, which is
        // the whole reason a separate anchor exists.
        val user = ChatTimelineProjector().project(CHAT, conversation())
            .filterIsInstance<ChatEntry.UserMessage>()
            .first()

        assertEquals(0L, user.sequence, "it is the first thing rendered")
        assertEquals(1L, user.messageSequence, "but the second thing stored")
    }

    @Test
    fun atoolResultDoesNotRestampTheCallItAnswers() {
        // The result arrives in a later turn, and completing the call must not re-anchor it there:
        // the entry that renders belongs to the turn that made the call.
        val call = ChatTimelineProjector().project(CHAT, conversation())
            .filterIsInstance<ChatEntry.ToolCall>()
            .single()

        assertTrue(call.status is ToolCallStatus.Completed, "the result should have landed")
        assertEquals(2L, call.messageSequence, "the calling turn, not the answering one")
    }

    private fun conversation(): List<Message> {
        val now = Clock.System.now()
        return listOf(
            Message.System(parts = listOf(MessagePart.Text("be helpful")), metaInfo = RequestMetaInfo(now)),
            Message.User(parts = listOf(MessagePart.Text("find my invoices")), metaInfo = RequestMetaInfo(now)),
            Message.Assistant(
                parts = listOf(
                    MessagePart.Reasoning(content = listOf("they want invoices")),
                    MessagePart.Tool.Call(id = "c1", tool = "searchInvoices", args = "{}"),
                    MessagePart.Text("Looking now."),
                ),
                metaInfo = ResponseMetaInfo(now),
            ),
            Message.User(
                parts = listOf(
                    MessagePart.Tool.Result(
                        id = "c1",
                        tool = "searchInvoices",
                        parts = listOf(MessagePart.Text("three found")),
                    ),
                ),
                metaInfo = RequestMetaInfo(now),
            ),
        )
    }

    private companion object {
        val CHAT = ChatId("chat-1")
    }
}
