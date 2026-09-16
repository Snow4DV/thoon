package com.mvlog.agent.impl.domain.repository

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatMetadata
import com.mvlog.agent.impl.domain.entity.ChatMetadataMatch
import kotlinx.coroutines.flow.Flow

internal interface ChatMetadataRepository {

    suspend fun create(chatId: ChatId, configId: AgentConfigId?)

    suspend fun get(chatId: ChatId): ChatMetadata?

    fun observe(chatId: ChatId): Flow<ChatMetadata?>

    fun observeAll(): Flow<List<ChatMetadata>>

    suspend fun setConfigId(chatId: ChatId, configId: AgentConfigId?)

    suspend fun clearConfigOverrides(configId: AgentConfigId)

    suspend fun setTitle(chatId: ChatId, title: String?)

    /** A blank query lists every chat. */
    fun search(query: String): Flow<List<ChatMetadataMatch>>

    suspend fun delete(chatId: ChatId)
}
