package com.mvlog.agent.impl.koog

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.snapshot.feature.isTombstone
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import com.mvlog.agent.impl.domain.repository.CheckpointRepository
import com.mvlog.log.TLogger

internal class PersistentChatHistoryProvider(
    private val historyRepository: ChatHistoryRepository,
    private val checkpointRepository: CheckpointRepository,
    private val historyCodec: KoogMessageRowCodec,
    private val checkpointCodec: CheckpointCodec,
) : ChatHistoryProvider {

    override suspend fun load(conversationId: String): List<Message> {
        val chatId = ChatId(conversationId)
        val messages = uncommittedMessages(chatId) ?: committedMessages(chatId)

        // Drop only the trailing prompt the run re-sends. An earlier unanswered prompt (a stopped or
        // failed run) is history the model must still see, and trailing tool results are what a
        // resumed turn needs.
        val last = messages.lastOrNull() ?: return messages
        val isPlainPrompt = last is Message.User && last.parts.none { it is MessagePart.Tool.Result }
        return if (isPlainPrompt) messages.dropLast(1) else messages
    }

    override suspend fun store(conversationId: String, messages: List<Message>) {
        historyRepository.commit(
            chatId = ChatId(conversationId),
            messages = historyCodec.toRows(messages),
        )
    }

    private suspend fun uncommittedMessages(chatId: ChatId): List<Message>? {
        val stored = checkpointRepository.latest(chatId) ?: return null
        val checkpoint = runCatching { checkpointCodec.decode(stored.payload) }
            .onFailure { TLogger.e(TAG, "Unreadable checkpoint for ${chatId.value}", it) }
            .getOrNull()
            ?: return null

        return checkpoint.takeIf { !it.isTombstone() }?.messageHistory
    }

    /**
     * Unreadable history is logged and dropped whole: half restored is worse than empty, and
     * throwing would brick the chat.
     */
    private suspend fun committedMessages(chatId: ChatId): List<Message> =
        runCatching { historyCodec.toMessages(historyRepository.load(chatId)) }
            .onFailure { TLogger.e(TAG, "Unreadable conversation for ${chatId.value}", it) }
            .getOrDefault(emptyList())

    private companion object {
        const val TAG = "ChatHistoryProvider"
    }
}
