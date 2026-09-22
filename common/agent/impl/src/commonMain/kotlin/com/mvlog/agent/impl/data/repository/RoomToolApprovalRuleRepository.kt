package com.mvlog.agent.impl.data.repository

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import com.mvlog.agent.impl.data.room.dao.ToolApprovalRuleDao
import com.mvlog.agent.impl.data.room.entity.ToolApprovalRuleEntity
import com.mvlog.agent.impl.domain.repository.ToolApprovalRuleRepository
import com.mvlog.log.TLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.time.Instant

internal class RoomToolApprovalRuleRepository(
    private val dao: ToolApprovalRuleDao,
    private val json: Json,
) : ToolApprovalRuleRepository {

    override fun observeScoped(chatId: ChatId?): Flow<List<ToolApprovalRule>> =
        (chatId?.let { dao.observeForChat(it.value) } ?: dao.observeGlobal()).map { it.toDomain() }

    override fun observeApplicable(chatId: ChatId): Flow<List<ToolApprovalRule>> =
        dao.observeApplicable(chatId.value).map { it.toDomain() }

    override suspend fun applicable(chatId: ChatId): List<ToolApprovalRule> =
        dao.applicable(chatId.value).toDomain()

    override suspend fun add(rule: ToolApprovalRule) {
        dao.upsert(
            ToolApprovalRuleEntity(
                id = rule.id.value,
                chatId = rule.chatId?.value,
                toolName = rule.toolName,
                parametersJson = json.encodeToString(PARAMETERS, rule.parameters),
                createdAt = rule.createdAt.toEpochMilliseconds(),
            )
        )
    }

    override suspend fun remove(id: ToolApprovalRuleId) {
        dao.delete(id.value)
    }

    override suspend fun deleteForChat(chatId: ChatId) {
        dao.deleteForChat(chatId.value)
    }

    // An unreadable rule is dropped, not widened: a rule with lost parameters would match too much.
    private fun List<ToolApprovalRuleEntity>.toDomain(): List<ToolApprovalRule> = mapNotNull { row ->
        runCatching { json.decodeFromString(PARAMETERS, row.parametersJson) }
            .onFailure { TLogger.e(TAG, "Unreadable approval rule ${row.id}", it) }
            .getOrNull()
            ?.let { parameters ->
                ToolApprovalRule(
                    id = ToolApprovalRuleId(row.id),
                    toolName = row.toolName,
                    chatId = row.chatId?.let(::ChatId),
                    parameters = parameters,
                    createdAt = Instant.fromEpochMilliseconds(row.createdAt),
                )
            }
    }

    private companion object {
        const val TAG = "ToolApprovalRules"
        val PARAMETERS = MapSerializer(String.serializer(), String.serializer())
    }
}
