package com.mvlog.agent.impl.domain.usecase

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.GetAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigOverrideUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase
import com.mvlog.agent.impl.domain.repository.AgentConfigRepository
import com.mvlog.agent.impl.domain.repository.ChatMetadataRepository
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class ObserveAgentConfigsUseCaseImpl(
    private val repository: AgentConfigRepository,
) : ObserveAgentConfigsUseCase {

    override fun invoke(): Flow<List<AgentConfig>> = repository.observeAll()
}

internal class GetAgentConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
) : GetAgentConfigUseCase {

    override suspend fun invoke(id: AgentConfigId): AgentConfig? = repository.get(id)
}

internal class ObserveDefaultAgentConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
) : ObserveDefaultAgentConfigUseCase {

    override fun invoke(): Flow<AgentConfig?> = repository.observeDefault()
}

internal class ObserveChatConfigOverrideUseCaseImpl(
    private val metadataRepository: ChatMetadataRepository,
) : ObserveChatConfigOverrideUseCase {

    override fun invoke(chatId: ChatId): Flow<AgentConfigId?> =
        metadataRepository.observe(chatId).map { it?.configId }.distinctUntilChanged()
}

internal class CreateAgentConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
    private val validate: ValidateAgentConfigDraftUseCase,
    private val idGenerator: IdGenerator,
) : CreateAgentConfigUseCase {

    override suspend fun invoke(draft: AgentConfigDraft): AgentConfigResult<AgentConfigId> {
        validate(draft)?.let { return AgentConfigResult.Failure(it) }

        val id = AgentConfigId(idGenerator.newId())
        val isFirst = repository.count() == 0
        repository.create(id, draft)

        if (isFirst) repository.setDefault(id)

        return AgentConfigResult.Success(id)
    }
}

internal class UpdateAgentConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
    private val validate: ValidateAgentConfigDraftUseCase,
) : UpdateAgentConfigUseCase {

    override suspend fun invoke(
        id: AgentConfigId,
        draft: AgentConfigDraft,
    ): AgentConfigResult<Unit> {
        validate(draft)?.let { return AgentConfigResult.Failure(it) }
        repository.get(id) ?: return AgentConfigResult.Failure(AgentConfigError.NotFound(id))

        repository.update(id, draft)
        return AgentConfigResult.Success(Unit)
    }
}

internal class DeleteAgentConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
    private val metadataRepository: ChatMetadataRepository,
) : DeleteAgentConfigUseCase {

    override suspend fun invoke(id: AgentConfigId) {
        // Chat overrides are detached here; the DAO only clears the default.
        metadataRepository.clearConfigOverrides(id)
        repository.delete(id)
    }
}

internal class SetDefaultAgentConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
) : SetDefaultAgentConfigUseCase {

    override suspend fun invoke(id: AgentConfigId): AgentConfigResult<Unit> {
        repository.get(id) ?: return AgentConfigResult.Failure(AgentConfigError.NotFound(id))
        repository.setDefault(id)
        return AgentConfigResult.Success(Unit)
    }
}

internal class SetChatConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
    private val metadataRepository: ChatMetadataRepository,
) : SetChatConfigUseCase {

    override suspend fun invoke(
        chatId: ChatId,
        configId: AgentConfigId?,
    ): AgentConfigResult<Unit> {
        if (configId != null) {
            repository.get(configId)
                ?: return AgentConfigResult.Failure(AgentConfigError.NotFound(configId))
        }
        metadataRepository.setConfigId(chatId, configId)
        return AgentConfigResult.Success(Unit)
    }
}

/**
 * No api counterpart: only the executor needs it. Resolved per run, so an in-flight run keeps the
 * config it began with.
 */
internal class ResolveAgentConfigUseCase(
    private val repository: AgentConfigRepository,
    private val metadataRepository: ChatMetadataRepository,
) {

    suspend operator fun invoke(chatId: ChatId): AgentConfig? {
        val override = metadataRepository.get(chatId)?.configId
        val resolved = override?.let { repository.get(it) }
        return resolved ?: repository.observeDefault().first()
    }
}
