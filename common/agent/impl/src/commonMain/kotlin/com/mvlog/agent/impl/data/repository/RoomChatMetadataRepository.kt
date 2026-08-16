package com.mvlog.agent.impl.data.repository

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.agent.impl.data.room.entity.ChatEntity
import com.mvlog.agent.impl.domain.entity.ChatMetadata
import com.mvlog.agent.impl.domain.repository.ChatMetadataRepository
import com.mvlog.agent.impl.util.AgentClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

internal class RoomChatMetadataRepository(
    private val dao: ChatDao,
    private val clock: AgentClock,
) : ChatMetadataRepository {

    override suspend fun create(chatId: ChatId, configId: AgentConfigId?) {
        val now = clock.now().toEpochMilliseconds()
        dao.upsert(
            ChatEntity(
                id = chatId.value,
                configId = configId?.value,
                title = null,
                historyJson = null,
                formatVersion = 0,
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    override suspend fun get(chatId: ChatId): ChatMetadata? = dao.get(chatId.value)?.toDomain()

    override fun observe(chatId: ChatId): Flow<ChatMetadata?> =
        dao.observe(chatId.value).map { it?.toDomain() }

    override fun observeAll(): Flow<List<ChatMetadata>> =
        dao.observeAll().map { chats -> chats.map { it.toDomain() } }

    override suspend fun setConfigId(chatId: ChatId, configId: AgentConfigId?) {
        dao.setConfigId(
            chatId = chatId.value,
            configId = configId?.value,
            updatedAt = clock.now().toEpochMilliseconds(),
        )
    }

    override suspend fun clearConfigOverrides(configId: AgentConfigId) {
        dao.clearConfigOverrides(configId.value)
    }

    override suspend fun setTitle(chatId: ChatId, title: String?) {
        dao.setTitle(
            chatId = chatId.value,
            title = title,
            updatedAt = clock.now().toEpochMilliseconds(),
        )
    }

    override suspend fun delete(chatId: ChatId) {
        dao.delete(chatId.value)
    }

    private fun ChatEntity.toDomain() = ChatMetadata(
        id = ChatId(id),
        title = title,
        configId = configId?.let(::AgentConfigId),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )
}
