package com.mvlog.agent.impl.data.repository

import com.mvlog.agent.api.model.AgentConfigId
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.data.room.dao.ChatDao
import com.mvlog.agent.impl.data.room.entity.ChatEntity
import com.mvlog.agent.impl.domain.entity.ChatMetadata
import com.mvlog.agent.impl.domain.entity.ChatMetadataMatch
import com.mvlog.agent.impl.domain.entity.escapeLike
import com.mvlog.agent.impl.domain.entity.snippetAround
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
                lastMessageAt = null,
                lastMessagePreview = null,
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

    /**
     * A blank query lists everything rather than matching nothing, so a search field can drive this
     * directly without the caller switching between two sources.
     *
     * The query is lowercased here because the stored column is: SQLite's `LIKE` folds case for
     * ASCII only, so 'привет' would otherwise never find 'Привет'.
     */
    override fun search(query: String): Flow<List<ChatMetadataMatch>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return observeAll().map { chats ->
                chats.map { ChatMetadataMatch(it, snippet = null, messageSequence = null) }
            }
        }

        // Lowercased to match `textLower`, then escaped: the order matters only in that both must
        // happen, and escaping cannot change the case of anything it introduces.
        return dao.search(query = escapeLike(trimmed.lowercase()), limit = SEARCH_LIMIT)
            .map { rows ->
                rows.groupBy { it.chat.id }
                    .flatMap { (_, chatRows) -> chatRows.take(MATCHES_PER_CHAT) }
                    .map { row ->
                        ChatMetadataMatch(
                            chat = row.chat.toDomain(),
                            snippet = snippetAround(row.matchedText, trimmed),
                            messageSequence = row.messageSequence,
                        )
                    }
            }
    }

    override suspend fun setTitle(chatId: ChatId, title: String?) {
        dao.setTitle(
            chatId = chatId.value,
            title = title,
            updatedAt = clock.now().toEpochMilliseconds(),
        )
    }

    override suspend fun delete(chatId: ChatId) {
        dao.deleteChat(chatId.value)
    }

    private fun ChatEntity.toDomain() = ChatMetadata(
        id = ChatId(id),
        title = title,
        configId = configId?.let(::AgentConfigId),
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        lastMessageAt = lastMessageAt?.let(Instant::fromEpochMilliseconds),
        lastMessagePreview = lastMessagePreview,
    )
}

/**
 * A common word matches every message there is, and this is a full scan — `LIKE '%…%'` cannot use an
 * index. The cap bounds what crosses the flow and gets mapped, not the scan itself.
 */
private const val SEARCH_LIMIT = 200

/**
 * How many hits one chat may contribute.
 *
 * Without it a single long conversation fills the entire list and every other chat that matched is
 * invisible. `groupBy` preserves encounter order, so the survivors are the earliest matches of each
 * chat in the query's own ordering. The trade-off is real and worth stating: a chat whose matches
 * all fall past the global limit contributes nothing at all.
 */
private const val MATCHES_PER_CHAT = 5
