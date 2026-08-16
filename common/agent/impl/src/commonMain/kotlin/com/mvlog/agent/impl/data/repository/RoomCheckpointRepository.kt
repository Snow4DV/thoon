package com.mvlog.agent.impl.data.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.data.room.dao.AgentCheckpointDao
import com.mvlog.agent.impl.data.room.entity.AgentCheckpointEntity
import com.mvlog.agent.impl.domain.repository.CheckpointRepository

internal class RoomCheckpointRepository(
    private val dao: AgentCheckpointDao,
    private val retainPerChat: Int = DEFAULT_RETAIN,
) : CheckpointRepository {

    override suspend fun latest(chatId: ChatId): CheckpointRepository.StoredCheckpoint? =
        dao.latest(chatId.value)?.toDomain()

    override suspend fun byId(
        chatId: ChatId,
        checkpointId: String,
    ): CheckpointRepository.StoredCheckpoint? =
        dao.byId(checkpointId)?.takeIf { it.sessionId == chatId.value }?.toDomain()

    override suspend fun all(chatId: ChatId): List<CheckpointRepository.StoredCheckpoint> =
        dao.forSession(chatId.value).map { it.toDomain() }

    override suspend fun save(chatId: ChatId, checkpoint: CheckpointRepository.StoredCheckpoint) {
        dao.save(
            checkpoint = AgentCheckpointEntity(
                checkpointId = checkpoint.checkpointId,
                sessionId = chatId.value,
                createdAt = checkpoint.createdAt,
                version = checkpoint.version,
                payloadJson = checkpoint.payload,
            ),
            keep = retainPerChat,
        )
    }

    override suspend fun chatsWithCheckpoints(): List<ChatId> =
        dao.sessionsWithCheckpoints().map(::ChatId)

    override suspend fun clear(chatId: ChatId) {
        dao.deleteForSession(chatId.value)
    }

    private fun AgentCheckpointEntity.toDomain() = CheckpointRepository.StoredCheckpoint(
        checkpointId = checkpointId,
        createdAt = createdAt,
        version = version,
        payload = payloadJson,
    )

    private companion object {
        /** A run commits and clears its journal; this only bounds an unusually long single run. */
        const val DEFAULT_RETAIN = 10
    }
}
