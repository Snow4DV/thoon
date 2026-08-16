package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatMetadata
import kotlinx.coroutines.flow.Flow

/**
 * The chat list and the per-chat settings around a conversation.
 *
 * Deliberately separate from [ChatHistoryRepository] even though both read the same row: this side
 * never touches the conversation payload, so nothing here can leak agent-framework concerns into a
 * chat list.
 */
internal interface ChatMetadataRepository {

    suspend fun create(chatId: ChatId, configId: AgentConfigId?)

    suspend fun get(chatId: ChatId): ChatMetadata?

    fun observe(chatId: ChatId): Flow<ChatMetadata?>

    fun observeAll(): Flow<List<ChatMetadata>>

    suspend fun setConfigId(chatId: ChatId, configId: AgentConfigId?)

    suspend fun clearConfigOverrides(configId: AgentConfigId)

    suspend fun setTitle(chatId: ChatId, title: String?)

    suspend fun delete(chatId: ChatId)
}
