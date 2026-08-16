package com.mvlog.agent.impl.koog

import ai.koog.agents.snapshot.feature.AgentCheckpointData
import ai.koog.agents.snapshot.feature.isTombstone
import ai.koog.agents.snapshot.providers.PersistenceStorageProvider
import ai.koog.agents.snapshot.providers.filters.AgentCheckpointPredicateFilter
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.repository.CheckpointRepository
import com.mvlog.log.TLogger

/**
 * Where the agent framework writes its checkpoints.
 *
 * `sessionId` is the chat id — it is whatever was passed as the session to the agent run, and the
 * framework keys everything by it.
 *
 * Nothing here deletes: checkpoints are cleared when a run commits its history, which is a single
 * transaction owned by [PersistentChatHistoryProvider]. The repository trims to a cap only as a
 * guard against one run producing an unusual number of them.
 */
internal class RoomPersistenceStorageProvider(
    private val repository: CheckpointRepository,
    private val codec: CheckpointCodec,
) : PersistenceStorageProvider<AgentCheckpointPredicateFilter> {

    override suspend fun saveCheckpoint(
        sessionId: String,
        agentCheckpointData: AgentCheckpointData,
    ) {
        // A tombstone marks the run as finished, which makes every checkpoint before it dead
        // weight — the conversation it described has already been committed to history.
        if (agentCheckpointData.isTombstone()) {
            repository.clear(ChatId(sessionId))
        }

        repository.save(
            chatId = ChatId(sessionId),
            checkpoint = CheckpointRepository.StoredCheckpoint(
                checkpointId = agentCheckpointData.checkpointId,
                createdAt = agentCheckpointData.createdAt.toEpochMilliseconds(),
                version = agentCheckpointData.version,
                payload = codec.encode(agentCheckpointData),
            ),
        )
    }

    override suspend fun getLatestCheckpoint(
        sessionId: String,
        filter: AgentCheckpointPredicateFilter?,
    ): AgentCheckpointData? =
        getCheckpoints(sessionId, filter).firstOrNull()

    override suspend fun getCheckpoints(
        sessionId: String,
        filter: AgentCheckpointPredicateFilter?,
    ): List<AgentCheckpointData> =
        repository.all(ChatId(sessionId))
            .mapNotNull { stored -> decodeOrNull(sessionId, stored) }
            // A null filter means "everything" — the framework passes one only when narrowing.
            .filter { filter == null || filter.check(it) }

    /**
     * A checkpoint we cannot read is skipped rather than fatal — one bad row should not make the
     * whole conversation unrecoverable.
     */
    private fun decodeOrNull(
        sessionId: String,
        stored: CheckpointRepository.StoredCheckpoint,
    ): AgentCheckpointData? =
        runCatching { codec.decode(stored.payload) }
            .onFailure {
                TLogger.e(TAG, "Unreadable checkpoint ${stored.checkpointId} for $sessionId", it)
            }
            .getOrNull()

    private companion object {
        const val TAG = "PersistenceStorage"
    }
}
