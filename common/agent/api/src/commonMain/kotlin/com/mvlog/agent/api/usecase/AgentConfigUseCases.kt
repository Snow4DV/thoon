package com.mvlog.agent.api.usecase

import com.mvlog.agent.api.model.AgentConfig
import com.mvlog.agent.api.model.AgentConfigDraft
import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.AgentConfigResult
import kotlinx.coroutines.flow.Flow

/** Everything a configuration screen needs. */

/** Every saved configuration, ordered by name. */
fun interface ObserveAgentConfigsUseCase {
    operator fun invoke(): Flow<List<AgentConfig>>
}

/** A single configuration, for an edit form. Emits null once it is deleted. */
fun interface ObserveAgentConfigUseCase {
    operator fun invoke(id: AgentConfigId): Flow<AgentConfig?>
}

/** The configuration chats fall back to when they have no override of their own. */
fun interface ObserveDefaultAgentConfigUseCase {
    operator fun invoke(): Flow<AgentConfig?>
}

/**
 * Validates and stores a new configuration.
 *
 * The first one created becomes the default, so a user who adds exactly one is not left with a
 * configuration that nothing uses.
 */
fun interface CreateAgentConfigUseCase {
    suspend operator fun invoke(draft: AgentConfigDraft): AgentConfigResult<AgentConfigId>
}

/** Validates and replaces the editable fields of an existing configuration. */
fun interface UpdateAgentConfigUseCase {
    suspend operator fun invoke(
        id: AgentConfigId,
        draft: AgentConfigDraft,
    ): AgentConfigResult<Unit>
}

/**
 * Removes a configuration, detaching it from the default and from every chat that selected it.
 *
 * Deleting the default leaves the app with no default rather than promoting another — an unset
 * default is visible to the user, a silently reassigned one is not.
 */
fun interface DeleteAgentConfigUseCase {
    suspend operator fun invoke(id: AgentConfigId)
}

fun interface SetDefaultAgentConfigUseCase {
    suspend operator fun invoke(id: AgentConfigId): AgentConfigResult<Unit>
}
