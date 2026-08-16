package com.mvlog.agent.impl.fake.memory

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.repository.CheckpointRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class InMemoryCheckpointRepository : CheckpointRepository {

    private val mutex = Mutex()

    private val checkpoints = mutableMapOf<ChatId, MutableList<CheckpointRepository.StoredCheckpoint>>()

    override suspend fun latest(chatId: ChatId): CheckpointRepository.StoredCheckpoint? =
        all(chatId).firstOrNull()

    override suspend fun byId(
        chatId: ChatId,
        checkpointId: String,
    ): CheckpointRepository.StoredCheckpoint? =
        all(chatId).firstOrNull { it.checkpointId == checkpointId }

    override suspend fun all(chatId: ChatId): List<CheckpointRepository.StoredCheckpoint> =
        mutex.withLock {
            checkpoints[chatId].orEmpty().sortedWith(
                compareByDescending<CheckpointRepository.StoredCheckpoint> { it.createdAt }
                    .thenByDescending { it.version }
            )
        }

    override suspend fun save(chatId: ChatId, checkpoint: CheckpointRepository.StoredCheckpoint) {
        mutex.withLock {
            val forChat = checkpoints.getOrPut(chatId) { mutableListOf() }
            forChat.removeAll { it.checkpointId == checkpoint.checkpointId }
            forChat += checkpoint
        }
    }

    override suspend fun chatsWithCheckpoints(): List<ChatId> =
        mutex.withLock { checkpoints.filterValues { it.isNotEmpty() }.keys.toList() }

    override suspend fun clear(chatId: ChatId) {
        mutex.withLock { checkpoints.remove(chatId) }
    }
}
