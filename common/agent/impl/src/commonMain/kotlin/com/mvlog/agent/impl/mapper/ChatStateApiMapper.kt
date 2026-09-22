package com.mvlog.agent.impl.mapper

import com.mvlog.agent.api.model.ChatExecutionState
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatItem
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolCallState
import com.mvlog.agent.impl.domain.approval.PendingToolCall
import com.mvlog.agent.impl.domain.approval.ToolApprovalResolver
import com.mvlog.agent.impl.domain.approval.ToolCallVerdict
import com.mvlog.agent.impl.domain.approval.toolCallKey
import com.mvlog.agent.impl.domain.entity.AgentRun
import com.mvlog.agent.impl.domain.entity.AgentRunStatus
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.entity.ToolCallStatus

internal class ChatStateApiMapper(
    private val approvals: ToolApprovalResolver,
) {

    fun map(
        chatId: ChatId,
        entries: List<ChatEntry>,
        runs: List<AgentRun>,
        decisions: Map<String, ToolApprovalDecision> = emptyMap(),
        rules: List<ToolApprovalRule> = emptyList(),
    ): ChatState {
        val approval = approvalView(entries, runs, decisions, rules)
        return ChatState(
            chatId = chatId,
            items = entries.flatMap { mapEntry(it, approval) },
            execution = approval?.let { ChatExecutionState.AwaitingApproval(it.undecided) }
                ?: mapExecution(runs),
        )
    }

    /** The trailing turn's tool calls when nothing has answered them and no run is live. */
    fun pendingTurn(entries: List<ChatEntry>): List<ChatEntry.ToolCall> {
        // A null anchor is a live run's entry; the projection is hydrated whole once it ends.
        if (entries.any { it.messageSequence == null }) return emptyList()
        val last = entries.maxOfOrNull { it.messageSequence ?: Long.MIN_VALUE } ?: return emptyList()
        val turn = entries.filter { it.messageSequence == last }
        if (turn.any { it is ChatEntry.UserMessage }) return emptyList()

        val calls = turn.filterIsInstance<ChatEntry.ToolCall>()
        val unanswered = calls.all {
            it.status is ToolCallStatus.Running ||
                it.status is ToolCallStatus.Pending ||
                it.status is ToolCallStatus.AwaitingApproval
        }
        return if (unanswered) calls else emptyList()
    }

    private class ApprovalView(
        val verdicts: Map<String, ToolCallVerdict>,
        /** Entry id to tool call key. */
        val keys: Map<String, String>,
        val decisions: Map<String, ToolApprovalDecision>,
    ) {
        val undecided: Int get() = verdicts.values.count { it is ToolCallVerdict.Undecided }
    }

    private fun approvalView(
        entries: List<ChatEntry>,
        runs: List<AgentRun>,
        decisions: Map<String, ToolApprovalDecision>,
        rules: List<ToolApprovalRule>,
    ): ApprovalView? {
        if (runs.any { !it.status.isTerminal }) return null

        val calls = pendingTurn(entries)
        if (calls.isEmpty()) return null

        val pending = calls.mapIndexed { ordinal, call ->
            PendingToolCall(
                key = toolCallKey(call.toolCallId, ordinal),
                id = call.toolCallId,
                name = call.name,
                argumentsJson = call.arguments.orEmpty(),
            )
        }
        if (!approvals.isGatedTurn(pending)) return null

        val verdicts = approvals.verdicts(pending, decisions, rules)
        if (verdicts.values.none { it is ToolCallVerdict.Undecided }) return null

        return ApprovalView(
            verdicts = verdicts,
            keys = calls.indices.associate { calls[it].id to pending[it].key },
            decisions = decisions,
        )
    }

    private fun mapEntry(entry: ChatEntry, approval: ApprovalView?): List<ChatItem> = when (entry) {
        is ChatEntry.UserMessage -> listOf(
            ChatItem.UserMessage(
                id = entry.id,
                messageSequence = entry.messageSequence,
                createdAt = entry.createdAt,
                text = entry.text,
            )
        )

        is ChatEntry.AssistantMessage -> listOf(
            ChatItem.AssistantMessage(
                id = entry.id,
                messageSequence = entry.messageSequence,
                createdAt = entry.createdAt,
                text = entry.text,
                isStreaming = entry.isStreaming,
            )
        )

        is ChatEntry.Reasoning -> listOf(
            ChatItem.Reasoning(
                id = entry.id,
                messageSequence = entry.messageSequence,
                createdAt = entry.createdAt,
                text = entry.text,
                isStreaming = entry.isStreaming,
            )
        )

        is ChatEntry.ToolCall -> mapToolCall(entry, approval)
    }

    private fun mapToolCall(entry: ChatEntry.ToolCall, approval: ApprovalView?): List<ChatItem> {
        val key = approval?.keys?.get(entry.id)
        val verdict = key?.let { approval.verdicts[it] }

        val call = ChatItem.ToolCall(
            id = entry.id,
            messageSequence = entry.messageSequence,
            createdAt = entry.createdAt,
            name = entry.name,
            arguments = entry.arguments,
            state = when (verdict) {
                null -> mapToolCallState(entry.status)
                ToolCallVerdict.Allowed -> ToolCallState.Pending
                else -> ToolCallState.AwaitingApproval
            },
        )

        if (key == null || verdict == null || verdict is ToolCallVerdict.Allowed) return listOf(call)

        return listOf(
            call,
            ChatItem.ToolApprovalRequest(
                id = "${entry.id}-approval",
                messageSequence = entry.messageSequence,
                createdAt = entry.createdAt,
                toolCallKey = key,
                toolName = entry.name,
                arguments = entry.arguments,
                decision = approval.decisions[key],
            ),
        )
    }

    private fun mapToolCallState(status: ToolCallStatus): ToolCallState = when (status) {
        ToolCallStatus.Pending -> ToolCallState.Pending
        ToolCallStatus.AwaitingApproval -> ToolCallState.AwaitingApproval
        ToolCallStatus.Running -> ToolCallState.Running
        is ToolCallStatus.Completed -> ToolCallState.Completed(status.result)
        is ToolCallStatus.Failed -> ToolCallState.Failed(status.error)
    }

    private fun mapExecution(runs: List<AgentRun>): ChatExecutionState {
        val active = runs.firstOrNull { it.status == AgentRunStatus.Running }
            ?: runs.firstOrNull { it.status == AgentRunStatus.Queued }

        if (active != null) {
            val queued = runs.count { it.status == AgentRunStatus.Queued && it.id != active.id }
            return ChatExecutionState.Working(
                runId = active.id,
                phase = when (active.status) {
                    AgentRunStatus.Running -> ChatExecutionState.Working.Phase.Running
                    else -> ChatExecutionState.Working.Phase.Starting
                },
                queuedPrompts = queued,
            )
        }

        // Only the latest run's failure is surfaced; anything older was superseded by a later run.
        val latest = runs.maxByOrNull { it.createdAt } ?: return ChatExecutionState.Idle
        return when (latest.status) {
            AgentRunStatus.Failed,
            AgentRunStatus.Interrupted,
            -> ChatExecutionState.Failed(runId = latest.id, message = latest.error)

            else -> ChatExecutionState.Idle
        }
    }
}
