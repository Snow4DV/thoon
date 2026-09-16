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

@OptIn(ExperimentalTime::class)
class KoogMessageRowCodecTest {

    private var next = 0
    private val codec = KoogMessageRowCodec(
        json = TestJson,
        idGenerator = IdGenerator { "id-${next++}" },
    )

    private val now = Clock.System.now()

    // The four turn shapes seen in a real conversation.
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
        assertEquals(
            listOf(StoredRole.System, StoredRole.User, StoredRole.Assistant, StoredRole.Assistant, StoredRole.User),
            codec.toRows(conversation).map { it.role },
        )
    }
}
