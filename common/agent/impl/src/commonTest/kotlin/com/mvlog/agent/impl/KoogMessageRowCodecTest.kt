package com.mvlog.agent.impl

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import com.mvlog.agent.impl.fake.TestJson
import com.mvlog.agent.impl.domain.entity.StoredPartKind
import com.mvlog.agent.impl.domain.entity.StoredRole
import com.mvlog.agent.impl.koog.KoogMessageRowCodec
import com.mvlog.agent.impl.util.IdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * The test the normalised design rests on.
 *
 * Conversations are no longer one blob; they are turns and parts in rows, and the framework must get
 * back exactly what it gave us — same order, same parts, same metadata. Fidelity of a part's
 * *contents* is the serialiser's job, so what this pins is the part we own: grouping, ordering, and
 * roles. A mistake here does not crash; it hands the model a subtly different conversation.
 *
 * The shapes below are the four that actually occur, taken from a real conversation:
 * `User[Text]`, `Assistant[Reasoning, Text]`, `Assistant[Reasoning, Call]`, `User[Result]`.
 */
@OptIn(ExperimentalTime::class)
class KoogMessageRowCodecTest {

    private var next = 0
    private val codec = KoogMessageRowCodec(
        json = TestJson,
        idGenerator = IdGenerator { "id-${next++}" },
    )

    private val now = Clock.System.now()

    private val conversation = listOf(
        Message.System(
            parts = listOf(MessagePart.Text("You are Thoon.")),
            metaInfo = RequestMetaInfo(now),
        ),
        Message.User(
            parts = listOf(MessagePart.Text("what is the fastest route?")),
            metaInfo = RequestMetaInfo(now),
        ),
        Message.Assistant(
            parts = listOf(
                MessagePart.Reasoning(content = listOf("The user wants a route. Let me think.")),
                MessagePart.Text("The M11."),
            ),
            metaInfo = ResponseMetaInfo(now),
        ),
        Message.Assistant(
            parts = listOf(
                MessagePart.Reasoning(content = listOf("I should check the file.")),
                MessagePart.Tool.Call(id = "call-1", tool = "read_file", args = """{"path":"a.md"}"""),
            ),
            metaInfo = ResponseMetaInfo(now),
        ),
        Message.User(
            parts = listOf(
                MessagePart.Tool.Result(
                    id = "call-1",
                    tool = "read_file",
                    parts = listOf(MessagePart.Text("file contents")),
                ),
            ),
            metaInfo = RequestMetaInfo(now),
        ),
    )

    @Test
    fun aConversationSurvivesTheRoundTrip() {
        assertEquals(conversation, codec.toMessages(codec.toRows(conversation)))
    }

    @Test
    fun partOrderInsideATurnIsPreserved() {
        // Reasoning precedes the answer it produced. Reordering would not fail anything loudly, it
        // would just change what the model reads back.
        val rebuilt = codec.toMessages(codec.toRows(conversation))
        val kinds = rebuilt[2].parts.map { it::class.simpleName }

        assertEquals(listOf("Reasoning", "Text"), kinds)
    }

    @Test
    fun turnsAreRebuiltInOrderEvenIfRowsArriveShuffled() {
        val rows = codec.toRows(conversation)

        assertEquals(conversation, codec.toMessages(rows.reversed()))
    }

    @Test
    fun onlyTextPartsCarryASearchableBody() {
        // The whole reason parts are rows: a search filters on this column, so reasoning and tool
        // output must never populate it. Otherwise searching finds what the model merely thought.
        val parts = codec.toRows(conversation).flatMap { it.parts }

        parts.filter { it.kind == StoredPartKind.Text }.forEach { assertEquals(true, it.text != null) }
        parts.filter { it.kind != StoredPartKind.Text }.forEach { assertNull(it.text) }
    }

    @Test
    fun toolCallsRecordTheirToolName() {
        val call = codec.toRows(conversation)
            .flatMap { it.parts }
            .single { it.kind == StoredPartKind.ToolCall }

        assertEquals("read_file", call.toolName)
    }

    @Test
    fun rolesAreRecordedSoSearchCanExcludeTheSystemPrompt() {
        // The system message is a Text part too. Without a role to filter on, every chat would match
        // any word in the system prompt.
        assertEquals(
            listOf(StoredRole.System, StoredRole.User, StoredRole.Assistant, StoredRole.Assistant, StoredRole.User),
            codec.toRows(conversation).map { it.role },
        )
    }
}
