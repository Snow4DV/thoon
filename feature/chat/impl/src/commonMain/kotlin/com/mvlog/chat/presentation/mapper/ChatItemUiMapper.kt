package com.mvlog.chat.presentation.mapper

import com.mvlog.agent.api.model.ToolCallState
import com.mvlog.chat.presentation.item.ChatItem
import kotlinx.collections.immutable.persistentListOf
import com.mvlog.agent.api.model.ChatItem as AgentChatItem

internal fun AgentChatItem.toUi(
    expandedIds: Set<String>,
): ChatItem = when (this) {
    is AgentChatItem.UserMessage -> ChatItem.Message(
        id = id,
        contentMarkdown = text,
        createdAt = createdAt,
        origin = ChatItem.Message.Origin.USER,
        attachments = persistentListOf(),
    )

    is AgentChatItem.AssistantMessage -> ChatItem.Message(
        id = id,
        contentMarkdown = text,
        createdAt = createdAt,
        origin = ChatItem.Message.Origin.AI,
        attachments = persistentListOf(),
    )

    is AgentChatItem.Reasoning -> ChatItem.Thought(
        id = id,
        createdAt = createdAt,
        // One reasoning stream becomes one bullet per line.
        thoughts = text.split("\n").filter { it.isNotBlank() },
        isExpanded = id in expandedIds,
    )

    is AgentChatItem.ToolCall -> ChatItem.ToolChainCall(
        id = id,
        createdAt = createdAt,
        toolName = name,
        action = arguments.orEmpty(),
        status = state.toUi(),
        isExpanded = id in expandedIds,
    )
}

private fun ToolCallState.toUi(): ChatItem.ToolChainCall.Status = when (this) {
    ToolCallState.Pending,
    ToolCallState.Running,
    -> ChatItem.ToolChainCall.Status.Loading

    is ToolCallState.Completed -> ChatItem.ToolChainCall.Status.Success(result)
    is ToolCallState.Failed -> ChatItem.ToolChainCall.Status.Failure(error)
}
