package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigId
import kotlinx.coroutines.flow.Flow

/**
 * Storage for connection profiles and the selections that point at them.
 *
 * Deals in the api-level [AgentConfig] directly: the model is already a plain description of user
 * input, so a parallel internal type would be a copy with no additional meaning.
 *
 * Per-chat overrides are not here — they are a property of the chat, owned by
 * [ChatMetadataRepository]. Resolving one against the default is a use case's job.
 */
internal interface AgentConfigRepository {

    fun observeAll(): Flow<List<AgentConfig>>

    fun observe(id: AgentConfigId): Flow<AgentConfig?>

    fun observeDefault(): Flow<AgentConfig?>


    suspend fun get(id: AgentConfigId): AgentConfig?


    suspend fun create(id: AgentConfigId, draft: AgentConfigDraft)

    suspend fun update(id: AgentConfigId, draft: AgentConfigDraft)

    /** Also clears the default and any chat overrides pointing at [id]. */
    suspend fun delete(id: AgentConfigId)

    suspend fun setDefault(id: AgentConfigId)


    suspend fun count(): Int
}
