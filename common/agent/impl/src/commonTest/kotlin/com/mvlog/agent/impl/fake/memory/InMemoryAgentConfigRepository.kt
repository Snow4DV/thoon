package com.mvlog.agent.impl.fake.memory

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.impl.domain.repository.AgentConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Deleting clears the default, as the Room DAO does; chat overrides are the use case's job. */
internal class InMemoryAgentConfigRepository : AgentConfigRepository {

    private val mutex = Mutex()

    private val configs = MutableStateFlow<Map<AgentConfigId, AgentConfig>>(emptyMap())

    private val defaultId = MutableStateFlow<AgentConfigId?>(null)

    override fun observeAll(): Flow<List<AgentConfig>> =
        configs.map { all -> all.values.sortedBy { it.name } }.distinctUntilChanged()

    override fun observeDefault(): Flow<AgentConfig?> =
        combine(configs, defaultId) { all, default -> default?.let(all::get) }
            .distinctUntilChanged()

    override suspend fun get(id: AgentConfigId): AgentConfig? = configs.value[id]

    override suspend fun create(id: AgentConfigId, draft: AgentConfigDraft) = mutex.withLock {
        configs.value += id to draft.toConfig(id)
    }

    override suspend fun update(id: AgentConfigId, draft: AgentConfigDraft) = mutex.withLock {
        configs.value[id] ?: return@withLock
        configs.value += id to draft.toConfig(id)
    }

    override suspend fun delete(id: AgentConfigId) = mutex.withLock {
        configs.value -= id
        if (defaultId.value == id) defaultId.value = null
    }

    override suspend fun setDefault(id: AgentConfigId) = mutex.withLock {
        defaultId.value = id
    }

    override suspend fun count(): Int = configs.value.size

    private fun AgentConfigDraft.toConfig(id: AgentConfigId): AgentConfig = when (this) {
        is AgentConfigDraft.OpenAiCompatible -> AgentConfig.OpenAiCompatible(
            id = id,
            name = name.trim(),
            modelId = modelId.trim(),
            baseUrl = baseUrl?.trim()?.ifBlank { null },
            apiKey = apiKey.trim(),
            chatCompletionsPath = chatCompletionsPath?.trim()?.ifBlank { null },
        )

        is AgentConfigDraft.Anthropic -> AgentConfig.Anthropic(
            id = id,
            name = name.trim(),
            modelId = modelId.trim(),
            baseUrl = baseUrl?.trim()?.ifBlank { null },
            apiKey = apiKey.trim(),
        )

        is AgentConfigDraft.Ollama -> AgentConfig.Ollama(
            id = id,
            name = name.trim(),
            modelId = modelId.trim(),
            baseUrl = baseUrl.trim(),
        )

        is AgentConfigDraft.Local -> AgentConfig.Local(
            id = id,
            name = name.trim(),
            modelId = modelId.trim(),
            engineId = engineId.trim(),
        )
    }
}
