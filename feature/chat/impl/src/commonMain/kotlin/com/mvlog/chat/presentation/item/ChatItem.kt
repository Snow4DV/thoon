package com.mvlog.chat.presentation.item

import com.mvlog.agent.api.model.ToolApprovalDecision
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

sealed interface ChatItem {

    val id: String

    data class Message(
        override val id: String,
        val contentMarkdown: String,
        val createdAt: Instant,
        val origin: Origin,
        val attachments: PersistentList<Attachment> = persistentListOf(),
        /** Still growing: the renderer appends instead of re-parsing. */
        val isStreaming: Boolean = false,
    ) : ChatItem {

        sealed interface Attachment {

            data class Text(val name: String, val downloadUrl: String) : Attachment
            data class Image(val name: String, val downloadUrl: String) : Attachment
            data class Binary(val name: String, val downloadUrl: String) : Attachment
        }

        enum class Origin {
            AI,
            USER,
        }
    }

    data class Thought(
        override val id: String,
        val createdAt: Instant,
        val thoughts: List<String>,
        val isExpanded: Boolean = false,
    ) : ChatItem

    data class ToolChainCall(
        override val id: String,
        val createdAt: Instant,
        val toolName: String,
        val action: String,
        val status: Status,
        val isExpanded: Boolean = false,
    ) : ChatItem {

        sealed interface Status {
            data object Loading : Status
            data object AwaitingApproval : Status
            data class Success(val result: String) : Status
            data class Failure(val errorMessage: String) : Status
        }
    }

    data class ToolApproval(
        override val id: String,
        val createdAt: Instant,
        val toolCallKey: String,
        val toolName: String,
        val action: String,
        val decision: ToolApprovalDecision?,
    ) : ChatItem
}
