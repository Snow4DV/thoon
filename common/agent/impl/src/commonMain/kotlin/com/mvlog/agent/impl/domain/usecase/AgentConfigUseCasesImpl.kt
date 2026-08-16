package com.mvlog.agent.impl.domain.usecase

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigError
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.usecase.CreateAgentConfigUseCase
import com.mvlog.agent.api.usecase.DeleteAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigUseCase
import com.mvlog.agent.api.usecase.ObserveAgentConfigsUseCase
import com.mvlog.agent.api.usecase.ObserveChatConfigUseCase
import com.mvlog.agent.api.usecase.ObserveDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.SetChatConfigUseCase
import com.mvlog.agent.api.usecase.SetDefaultAgentConfigUseCase
import com.mvlog.agent.api.usecase.UpdateAgentConfigUseCase
import com.mvlog.agent.impl.domain.repository.AgentConfigRepository
import com.mvlog.agent.impl.domain.repository.ChatMetadataRepository
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * Configuration use cases: the operations a settings screen performs, implemented against storage.
 *
 * Each implements the contract of the same name in `common:agent:api`.
 */
internal class ObserveAgentConfigsUseCaseImpl(
    private val repository: AgentConfigRepository,
) : ObserveAgentConfigsUseCase {

    override fun invoke(): Flow<List<AgentConfig>> = repository.observeAll()
}

internal class ObserveAgentConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
) : ObserveAgentConfigUseCase {

    override fun invoke(id: AgentConfigId): Flow<AgentConfig?> = repository.observe(id)
}

internal class ObserveDefaultAgentConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
) : ObserveDefaultAgentConfigUseCase {

    override fun invoke(): Flow<AgentConfig?> = repository.observeDefault()
}

/**
 * The configuration a chat runs on: its own override, else the app default.
 *
 * Composed here rather than stored, because the override belongs to the chat and the default
 * belongs to the configuration store — neither repository can answer this alone.
 */
internal class ObserveChatConfigUseCaseImpl(
    private val repository: AgentConfigRepository,
    private val metadataRepository: ChatMetadataRepository,
) : ObserveChatConfigUseCase {

    override fun invoke(chatId: ChatId): Flow<AgentConfig?> =
        combine(
            metadataRepository.observe(chatId),
            repository.observeAll(),
            repository.observeDefault(),
        ) { chat, configs, default ->
            val override = chat?.configId ?: return@combine default
            configs.firstOrNull { it.id == override } ?: default
        }.distinctUntilChanged()
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

        // The first configuration becomes the default: otherwise a user who adds exactly one is
        // left with a config that nothing uses, for no reason they can see.
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
        // Detach before deleting: a chat pointing at a configuration that no longer exists would
        // resolve to nothing rather than falling back to the default.
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
 * Which configuration a run should use: the chat's override, else the default, else none.
 *
 * Internal to the module — the execution layer needs it, but no screen does, so it has no
 * counterpart in `common:agent:api`.
 *
 * Resolved once per run rather than held, so a run started after a configuration change uses the
 * new value while an in-flight run keeps the one it began with.
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
