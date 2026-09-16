package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigId
import kotlinx.coroutines.flow.Flow

internal interface AgentConfigRepository {

    fun observeAll(): Flow<List<AgentConfig>>

    fun observeDefault(): Flow<AgentConfig?>

    suspend fun get(id: AgentConfigId): AgentConfig?

    suspend fun create(id: AgentConfigId, draft: AgentConfigDraft)

    suspend fun update(id: AgentConfigId, draft: AgentConfigDraft)

    /** Also clears the default if it pointed here. Chat overrides are the use case's job. */
    suspend fun delete(id: AgentConfigId)

    suspend fun setDefault(id: AgentConfigId)

    suspend fun count(): Int
}
