package com.mvlog.agent.impl.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.mvlog.agent.impl.data.room.entity.AgentCheckpointEntity
import com.mvlog.database.dao.ThoonDao

@Dao
interface AgentCheckpointDao : ThoonDao {

    @Upsert
    suspend fun upsert(checkpoint: AgentCheckpointEntity)

    @Query(
        """
        SELECT * FROM agent_checkpoint
        WHERE sessionId = :sessionId
        ORDER BY createdAt DESC, version DESC
        """
    )
    suspend fun forSession(sessionId: String): List<AgentCheckpointEntity>

    @Query(
        """
        SELECT * FROM agent_checkpoint
        WHERE sessionId = :sessionId
        ORDER BY createdAt DESC, version DESC
        LIMIT 1
        """
    )
    suspend fun latest(sessionId: String): AgentCheckpointEntity?

    @Query("SELECT * FROM agent_checkpoint WHERE checkpointId = :checkpointId")
    suspend fun byId(checkpointId: String): AgentCheckpointEntity?

    @Query("SELECT DISTINCT sessionId FROM agent_checkpoint")
    suspend fun sessionsWithCheckpoints(): List<String>

    @Query("DELETE FROM agent_checkpoint WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    /** Safety net for an unusually long run; commit clears the rest. */
    @Query(
        """
        DELETE FROM agent_checkpoint
        WHERE sessionId = :sessionId AND checkpointId NOT IN (
            SELECT checkpointId FROM agent_checkpoint
            WHERE sessionId = :sessionId
            ORDER BY createdAt DESC, version DESC
            LIMIT :keep
        )
        """
    )
    suspend fun trimSession(sessionId: String, keep: Int)

    @Transaction
    suspend fun save(checkpoint: AgentCheckpointEntity, keep: Int) {
        upsert(checkpoint)
        trimSession(sessionId = checkpoint.sessionId, keep = keep)
    }
}
