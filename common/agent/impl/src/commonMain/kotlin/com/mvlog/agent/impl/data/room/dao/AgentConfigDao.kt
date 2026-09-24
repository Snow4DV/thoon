package com.mvlog.agent.impl.data.room.dao

import androidx.room3.Dao
import com.mvlog.database.dao.ThoonDao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.mvlog.agent.impl.data.room.entity.AgentConfigEntity
import com.mvlog.agent.impl.data.room.entity.AgentSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentConfigDao : ThoonDao {

    @Query("SELECT * FROM agent_config ORDER BY name")
    fun observeAll(): Flow<List<AgentConfigEntity>>

    @Query("SELECT * FROM agent_config WHERE id = :configId")
    suspend fun get(configId: String): AgentConfigEntity?

    @Query("SELECT COUNT(*) FROM agent_config")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(config: AgentConfigEntity)

    @Query("DELETE FROM agent_config WHERE id = :configId")
    suspend fun delete(configId: String)

    @Query("SELECT * FROM agent_settings WHERE id = :id")
    fun observeSettings(id: Int = AgentSettingsEntity.SINGLETON_ID): Flow<AgentSettingsEntity?>

    @Query("SELECT * FROM agent_settings WHERE id = :id")
    suspend fun getSettings(id: Int = AgentSettingsEntity.SINGLETON_ID): AgentSettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: AgentSettingsEntity)

    /**
     * One transaction, so the default never points at a deleted row. Chat overrides are the use
     * case's job.
     */
    @Transaction
    suspend fun deleteAndDetach(configId: String) {
        delete(configId)
        val settings = getSettings()
        if (settings?.defaultConfigId == configId) {
            upsertSettings(settings.copy(defaultConfigId = null))
        }
    }
}
