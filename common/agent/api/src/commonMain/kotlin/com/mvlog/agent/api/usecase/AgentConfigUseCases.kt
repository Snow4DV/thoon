package com.mvlog.agent.api.usecase

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import kotlinx.coroutines.flow.Flow

/** Every saved configuration, ordered by name. */
fun interface ObserveAgentConfigsUseCase {
    operator fun invoke(): Flow<List<AgentConfig>>
}

/** One-shot, for seeding a form; null when it does not exist. */
fun interface GetAgentConfigUseCase {
    suspend operator fun invoke(id: AgentConfigId): AgentConfig?
}

/** The fallback for chats without an override; null when none is set. */
fun interface ObserveDefaultAgentConfigUseCase {
    operator fun invoke(): Flow<AgentConfig?>
}

/** The first configuration created becomes the default. */
fun interface CreateAgentConfigUseCase {
    suspend operator fun invoke(draft: AgentConfigDraft): AgentConfigResult<AgentConfigId>
}

fun interface UpdateAgentConfigUseCase {
    suspend operator fun invoke(
        id: AgentConfigId,
        draft: AgentConfigDraft,
    ): AgentConfigResult<Unit>
}

/** Detaches from the default and every chat; deleting the default leaves none set. */
fun interface DeleteAgentConfigUseCase {
    suspend operator fun invoke(id: AgentConfigId)
}

fun interface SetDefaultAgentConfigUseCase {
    suspend operator fun invoke(id: AgentConfigId): AgentConfigResult<Unit>
}
