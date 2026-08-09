package com.mvlog.chat.presentation.item

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

sealed interface ChatItem {

    data class Message(
        val id: String,
        val contentMarkdown: String,
        val createdAt: Instant,
        val origin: Origin,
        val attachments: PersistentList<Attachment> = persistentListOf(),
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
        val id: String,
        val createdAt: Instant,
        val thoughts: List<String>,
    ) : ChatItem

    data class ToolChainCall(
        val id: String,
        val createdAt: Instant,
        val toolName: String,
        val action: String,
        val status: Status,
    ) : ChatItem {

        sealed interface Status {
            data object Loading : Status
            data class Success(val result: String) : Status
            data class Failure(val errorMessage: String) : Status
        }
    }
}
