package com.mvlog.chat.presentation.mapper

import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalScope
import com.mvlog.agent.api.model.ToolCallState
import com.mvlog.chat.presentation.item.ChatItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant
import com.mvlog.agent.api.model.ChatItem as AgentChatItem

class ChatItemUiMapperTest {

    @Test
    fun aCallAwaitingApprovalKeepsItsOwnStatusRatherThanSpinning() {
        val call = AgentChatItem.ToolCall(
            id = "1",
            messageSequence = 3,
            createdAt = Instant.fromEpochSeconds(0),
            name = "write_file",
            arguments = "{}",
            state = ToolCallState.AwaitingApproval,
        )

        val status = assertIs<ChatItem.ToolChainCall>(call.toUi(emptySet())).status
        assertEquals(ChatItem.ToolChainCall.Status.AwaitingApproval, status)
    }

    @Test
    fun anApprovalRequestBecomesItsOwnItemCarryingTheKeyToAnswerWith() {
        val request = AgentChatItem.ToolApprovalRequest(
            id = "1-approval",
            messageSequence = 3,
            createdAt = Instant.fromEpochSeconds(0),
            toolCallKey = "call_9",
            toolName = "write_file",
            arguments = """{"path":"a.md"}""",
            decision = ToolApprovalDecision.Approve(ToolApprovalScope.Once),
        )

        val item = assertIs<ChatItem.ToolApproval>(request.toUi(emptySet()))
        assertEquals("call_9", item.toolCallKey, "the key is what the decision is sent back with")
        assertEquals("""{"path":"a.md"}""", item.action)
        assertEquals(ToolApprovalDecision.Approve(ToolApprovalScope.Once), item.decision)
    }
}
