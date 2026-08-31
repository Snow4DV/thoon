package com.mvlog.agent.impl.koog

import ai.koog.agents.snapshot.feature.isTombstone
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.RequestMetaInfo
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatEntry
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import com.mvlog.agent.impl.domain.repository.ChatMetadataRepository
import com.mvlog.agent.impl.domain.repository.CheckpointRepository
import com.mvlog.agent.impl.domain.repository.ConversationRepository
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.log.TLogger
import kotlinx.coroutines.flow.first

/**
 * Reads and writes the durable conversation, and is the only place outside the runner that knows it
 * is stored as agent-framework messages.
 *
 * Uncommitted work wins over committed history — the same rule [PersistentChatHistoryProvider]
 * restores the model with, so the timeline and the replay can never diverge.
 */
internal class KoogConversationRepository(
    private val historyRepository: ChatHistoryRepository,
    private val checkpointRepository: CheckpointRepository,
    private val metadataRepository: ChatMetadataRepository,
    private val historyCodec: KoogMessageRowCodec,
    private val checkpointCodec: CheckpointCodec,
    private val projector: ChatTimelineProjector,
    private val clock: AgentClock,
) : ConversationRepository {

    override suspend fun timeline(chatId: ChatId): List<ChatEntry> =
        projector.project(chatId, messages(chatId))

    override suspend fun appendUserPrompt(chatId: ChatId, text: String) {
        // Committed history only: a checkpoint belongs to the run that produced it, and appending
        // to it would rewrite that run's own record of what it was doing.
        val committed = committedMessages(chatId)
        val appended = committed + Message.User(
            content = text,
            metaInfo = RequestMetaInfo(timestamp = clock.now()),
        )

        historyRepository.commit(chatId = chatId, messages = historyCodec.toRows(appended))
    }

    override suspend fun unansweredPrompt(chatId: ChatId): String? =
        messages(chatId).lastOrNull()
            ?.takeIf { it is Message.User }
            ?.textContent()
            ?.takeIf { it.isNotBlank() }

    /**
     * True when the conversation ends with tool results.
     *
     * Checked by parts rather than by text: a tool result is a `Message.User` whose content is the
     * result payload, so [unansweredPrompt] reads it as a prompt with nothing in it and skips the
     * chat entirely.
     */
    override suspend fun hasUnfinishedToolTurn(chatId: ChatId): Boolean =
        messages(chatId).lastOrNull()
            ?.let { last -> last is Message.User && last.parts.any { it is MessagePart.Tool.Result } }
            ?: false

    override suspend fun chatsWithUnansweredPrompts(): List<ChatId> =
        metadataRepository.observeAll().first()
            .map { it.id }
            .filter { unansweredPrompt(it) != null || hasUnfinishedToolTurn(it) }

    /** Uncommitted work if a run left any, else the committed conversation. */
    private suspend fun messages(chatId: ChatId): List<Message> =
        uncommittedMessages(chatId) ?: committedMessages(chatId)

    private suspend fun uncommittedMessages(chatId: ChatId): List<Message>? {
        val stored = checkpointRepository.latest(chatId) ?: return null
        val checkpoint = runCatching { checkpointCodec.decode(stored.payload) }
            .onFailure { TLogger.e(TAG, "Unreadable checkpoint for ${chatId.value}", it) }
            .getOrNull()
            ?: return null

        // A tombstone records that a run ended and carries no history of its own.
        return checkpoint.takeIf { !it.isTombstone() }?.messageHistory
    }

    private suspend fun committedMessages(chatId: ChatId): List<Message> =
        runCatching { historyCodec.toMessages(historyRepository.load(chatId)) }
            .onFailure { TLogger.e(TAG, "Unreadable conversation for ${chatId.value}", it) }
            .getOrDefault(emptyList())

    private companion object {
        const val TAG = "ConversationRepository"
    }
}
