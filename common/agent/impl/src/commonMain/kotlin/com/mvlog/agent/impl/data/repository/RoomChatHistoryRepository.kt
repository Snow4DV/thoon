package com.mvlog.agent.impl.data.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.agent.impl.data.room.entity.ChatMessageEntity
import com.mvlog.agent.impl.data.room.entity.ChatPartEntity
import com.mvlog.agent.impl.domain.entity.ConversationDiff
import com.mvlog.agent.impl.domain.entity.StoredMessage
import com.mvlog.agent.impl.domain.entity.StoredMessageBoundary
import com.mvlog.agent.impl.domain.entity.conversationDiff
import com.mvlog.agent.impl.domain.entity.StoredPart
import com.mvlog.agent.impl.domain.entity.StoredPartKind
import com.mvlog.agent.impl.domain.entity.StoredRole
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.log.TLogger

internal class RoomChatHistoryRepository(
    private val dao: ChatDao,
    private val clock: AgentClock,
) : ChatHistoryRepository {

    override suspend fun load(chatId: ChatId): List<StoredMessage> {
        val messages = dao.messages(chatId.value)
        if (messages.isEmpty()) return emptyList()

        val partsByMessage = dao.parts(chatId.value).groupBy { it.messageId }

        return messages.mapNotNull { message ->
            val role = StoredRole.of(message.role)
            if (role == null) {
                // Drop the whole history, not the turn: a partial replay silently changes what the
                // model sees.
                TLogger.e(TAG, "Unknown role '${message.role}' in ${chatId.value}; dropping history")
                return emptyList()
            }

            StoredMessage(
                id = message.id,
                sequence = message.sequence,
                role = role,
                metaInfoJson = message.metaInfoJson,
                parts = partsByMessage[message.id].orEmpty().mapNotNull { it.toDomain(chatId) },
            )
        }
    }

    override suspend fun commit(chatId: ChatId, messages: List<StoredMessage>) {
        val boundary = dao.lastMessage(chatId.value)?.let { last ->
            StoredRole.of(last.role)?.let { role ->
                StoredMessageBoundary(id = last.id, sequence = last.sequence, role = role)
            }
        }

        val diff = conversationDiff(
            incoming = messages,
            storedCount = dao.messageCount(chatId.value),
            boundary = boundary,
        )

        val fromSequence = when (diff) {
            is ConversationDiff.Append -> diff.fromSequence
            ConversationDiff.Rewrite -> {
                TLogger.i(TAG, "Conversation for ${chatId.value} no longer extends storage; rewriting")
                0L
            }
        }

        val incoming = messages.filter { it.sequence >= fromSequence }
        val lastSpoken = messages.lastOrNull { it.role != StoredRole.System }
        val now = clock.now().toEpochMilliseconds()

        dao.commitConversation(
            chatId = chatId.value,
            fromSequence = fromSequence,
            messages = incoming.map { it.toEntity(chatId) },
            parts = incoming.flatMap { message -> message.parts.map { it.toEntity(message.id) } },
            lastMessageAt = lastSpoken?.let { now },
            preview = lastSpoken?.preview(),
            updatedAt = now,
        )
    }

    override suspend fun clear(chatId: ChatId) {
        dao.commitConversation(
            chatId = chatId.value,
            fromSequence = 0L,
            messages = emptyList(),
            parts = emptyList(),
            lastMessageAt = null,
            preview = null,
            updatedAt = clock.now().toEpochMilliseconds(),
        )
    }

    private fun StoredMessage.preview(): String? =
        parts.firstOrNull { it.kind == StoredPartKind.Text }?.text?.take(PREVIEW_LENGTH)

    private fun StoredMessage.toEntity(chatId: ChatId) = ChatMessageEntity(
        id = id,
        chatId = chatId.value,
        sequence = sequence,
        role = role.stored,
        metaInfoJson = metaInfoJson,
        createdAt = clock.now().toEpochMilliseconds(),
    )

    private fun StoredPart.toEntity(messageId: String) = ChatPartEntity(
        id = id,
        messageId = messageId,
        sequence = sequence,
        kind = kind.stored,
        text = text,
        textLower = text?.lowercase(),
        toolName = toolName,
        payloadJson = payloadJson,
    )

    private fun ChatPartEntity.toDomain(chatId: ChatId): StoredPart? {
        val partKind = StoredPartKind.of(kind) ?: run {
            TLogger.e(TAG, "Unknown part kind '$kind' in ${chatId.value}")
            return null
        }

        return StoredPart(
            id = id,
            sequence = sequence,
            kind = partKind,
            text = text,
            toolName = toolName,
            payloadJson = payloadJson,
        )
    }

    private companion object {
        const val TAG = "ChatHistory"
        const val PREVIEW_LENGTH = 120
    }
}
