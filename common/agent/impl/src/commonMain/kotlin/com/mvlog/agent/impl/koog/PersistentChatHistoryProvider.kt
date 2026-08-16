package com.mvlog.agent.impl.koog

import ai.koog.agents.chatMemory.feature.ChatHistoryProvider
import ai.koog.agents.snapshot.feature.isTombstone
import ai.koog.prompt.message.Message
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.repository.ChatHistoryRepository
import com.mvlog.agent.impl.domain.repository.CheckpointRepository
import com.mvlog.log.TLogger

/**
 * Restores a conversation for the agent, and commits it when a run finishes.
 *
 * [load] prefers the latest checkpoint over committed history. That is what lets a run that died
 * mid-execution still contribute: its tool calls and their results are in the checkpoint, so the
 * next prompt is answered with them in context instead of pretending the run never happened.
 *
 * [store] is the commit — it makes the conversation durable and drops the checkpoints behind it in
 * the same transaction.
 */
internal class PersistentChatHistoryProvider(
    private val historyRepository: ChatHistoryRepository,
    private val checkpointRepository: CheckpointRepository,
    private val historyCodec: KoogHistoryCodec,
    private val checkpointCodec: CheckpointCodec,
) : ChatHistoryProvider {

    override suspend fun load(conversationId: String): List<Message> {
        val chatId = ChatId(conversationId)
        val messages = uncommittedMessages(chatId) ?: committedMessages(chatId)

        // A trailing user message is the prompt this run is about to send as its input; keeping it
        // here as well would show the model the same turn twice.
        return messages.dropLastWhile { it is Message.User }
    }

    override suspend fun store(conversationId: String, messages: List<Message>) {
        historyRepository.commit(
            chatId = ChatId(conversationId),
            formatVersion = KoogHistoryCodec.FORMAT_VERSION,
            payload = historyCodec.encode(messages),
        )
    }

    /**
     * Work from a run that never committed, or null when there is none.
     *
     * A tombstone marks a run that ended, and carries no message history — treating it as partial
     * work would hand the model an empty conversation and erase everything already committed.
     */
    private suspend fun uncommittedMessages(chatId: ChatId): List<Message>? {
        val stored = checkpointRepository.latest(chatId) ?: return null
        val checkpoint = runCatching { checkpointCodec.decode(stored.payload) }
            .onFailure { TLogger.e(TAG, "Unreadable checkpoint for ${chatId.value}", it) }
            .getOrNull()
            ?: return null

        return checkpoint.takeIf { !it.isTombstone() }?.messageHistory
    }

    private suspend fun committedMessages(chatId: ChatId): List<Message> {
        val stored = historyRepository.load(chatId) ?: return emptyList()

        if (stored.formatVersion != KoogHistoryCodec.FORMAT_VERSION) {
            TLogger.i(
                TAG,
                "Ignoring history for ${chatId.value}: format ${stored.formatVersion} " +
                    "is not ${KoogHistoryCodec.FORMAT_VERSION}",
            )
            return emptyList()
        }

        // Starting fresh loses context; throwing would make the chat unusable altogether.
        return runCatching { historyCodec.decode(stored.payload) }
            .onFailure { TLogger.e(TAG, "Corrupt history for ${chatId.value}", it) }
            .getOrDefault(emptyList())
    }

    private companion object {
        const val TAG = "ChatHistoryProvider"
    }
}
