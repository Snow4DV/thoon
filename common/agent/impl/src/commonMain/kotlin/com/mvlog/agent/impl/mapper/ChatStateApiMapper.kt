package com.mvlog.agent.impl.mapper

import com.mvlog.agent.api.model.ChatExecutionState
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ChatItem
import com.mvlog.agent.api.model.ChatState
import com.mvlog.agent.api.model.ToolCallState
import com.mvlog.agent.impl.domain.entity.AgentRun
import com.mvlog.agent.impl.domain.entity.AgentRunStatus
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.entity.ToolCallStatus

internal class ChatStateApiMapper {

    fun map(chatId: ChatId, entries: List<ChatEntry>, runs: List<AgentRun>): ChatState =
        ChatState(
            chatId = chatId,
            items = entries.map(::mapEntry),
            execution = mapExecution(runs),
        )

    private fun mapEntry(entry: ChatEntry): ChatItem = when (entry) {
        is ChatEntry.UserMessage -> ChatItem.UserMessage(
            id = entry.id,
            messageSequence = entry.messageSequence,
            createdAt = entry.createdAt,
            text = entry.text,
        )

        is ChatEntry.AssistantMessage -> ChatItem.AssistantMessage(
            id = entry.id,
            messageSequence = entry.messageSequence,
            createdAt = entry.createdAt,
            text = entry.text,
            isStreaming = entry.isStreaming,
        )

        is ChatEntry.Reasoning -> ChatItem.Reasoning(
            id = entry.id,
            messageSequence = entry.messageSequence,
            createdAt = entry.createdAt,
            text = entry.text,
            isStreaming = entry.isStreaming,
        )

        is ChatEntry.ToolCall -> ChatItem.ToolCall(
            id = entry.id,
            messageSequence = entry.messageSequence,
            createdAt = entry.createdAt,
            name = entry.name,
            arguments = entry.arguments,
            state = mapToolCallState(entry.status),
        )
    }

    private fun mapToolCallState(status: ToolCallStatus): ToolCallState = when (status) {
        ToolCallStatus.Pending -> ToolCallState.Pending
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