package com.mvlog.agent.impl.fake.memory

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.entity.ChatMetadata
import com.mvlog.agent.impl.domain.repository.ChatMetadataRepository
import com.mvlog.agent.impl.util.AgentClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class InMemoryChatMetadataRepository(
    private val clock: AgentClock = AgentClock.System,
) : ChatMetadataRepository {

    private val chats = MutableStateFlow<Map<ChatId, ChatMetadata>>(emptyMap())

    override suspend fun create(chatId: ChatId, configId: AgentConfigId?) {
        val now = clock.now()
        chats.value += chatId to ChatMetadata(
            id = chatId,
            title = null,
            configId = configId,
            createdAt = now,
            updatedAt = now,
        )
    }

    override suspend fun get(chatId: ChatId): ChatMetadata? = chats.value[chatId]

    override fun observe(chatId: ChatId): Flow<ChatMetadata?> =
        chats.map { it[chatId] }.distinctUntilChanged()

    override fun observeAll(): Flow<List<ChatMetadata>> =
        chats.map { all -> all.values.sortedByDescending { it.updatedAt } }.distinctUntilChanged()

    override suspend fun setConfigId(chatId: ChatId, configId: AgentConfigId?) {
        update(chatId) { it.copy(configId = configId, updatedAt = clock.now()) }
    }

    override suspend fun clearConfigOverrides(configId: AgentConfigId) {
        chats.value = chats.value.mapValues { (_, chat) ->
            if (chat.configId == configId) chat.copy(configId = null) else chat
        }
    }

    override suspend fun setTitle(chatId: ChatId, title: String?) {
        update(chatId) { it.copy(title = title, updatedAt = clock.now()) }
    }

    override suspend fun delete(chatId: ChatId) {
        chats.value -= chatId
    }

    private fun update(chatId: ChatId, transform: (ChatMetadata) -> ChatMetadata) {
        val existing = chats.value[chatId] ?: return
        chats.value += chatId to transform(existing)
    }
}
