package com.mvlog.agent.impl.data.repository

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.impl.data.mapper.AgentConfigMapper
import com.mvlog.agent.impl.data.room.dao.AgentConfigDao
import com.mvlog.agent.impl.data.room.entity.AgentSettingsEntity
import com.mvlog.agent.impl.domain.repository.AgentConfigRepository
import com.mvlog.agent.impl.util.AgentClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class RoomAgentConfigRepository(
    private val dao: AgentConfigDao,
    private val mapper: AgentConfigMapper,
    private val clock: AgentClock,
) : AgentConfigRepository {

    override fun observeAll(): Flow<List<AgentConfig>> =
        dao.observeAll().map { configs -> configs.map(mapper::toDomain) }

    override fun observeDefault(): Flow<AgentConfig?> =
        combine(dao.observeSettings(), dao.observeAll()) { settings, configs ->
            val defaultId = settings?.defaultConfigId ?: return@combine null
            configs.firstOrNull { it.id == defaultId }?.let(mapper::toDomain)
        }.distinctUntilChanged()

    override suspend fun get(id: AgentConfigId): AgentConfig? =
        dao.get(id.value)?.let(mapper::toDomain)

    override suspend fun create(id: AgentConfigId, draft: AgentConfigDraft) {
        dao.upsert(mapper.toEntity(id = id, draft = draft))
    }

    override suspend fun update(id: AgentConfigId, draft: AgentConfigDraft) {
        dao.get(id.value) ?: return
        dao.upsert(mapper.toEntity(id = id, draft = draft))
    }

    override suspend fun delete(id: AgentConfigId) {
        dao.deleteAndDetach(id.value)
    }

    override suspend fun setDefault(id: AgentConfigId) {
        dao.upsertSettings(
            AgentSettingsEntity(
                id = AgentSettingsEntity.SINGLETON_ID,
                defaultConfigId = id.value,
            )
        )
    }

    override suspend fun count(): Int = dao.count()
}
