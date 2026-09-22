package com.mvlog.agent.impl.domain.usecase

import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.api.model.ToolApprovalDecision
import com.mvlog.agent.api.model.ToolApprovalRule
import com.mvlog.agent.api.model.ToolApprovalRuleId
import com.mvlog.agent.api.model.ToolApprovalScope
import com.mvlog.agent.api.usecase.AddToolApprovalRuleUseCase
import com.mvlog.agent.api.usecase.DecideToolCallUseCase
import com.mvlog.agent.api.usecase.GetApprovalGatedToolsUseCase
import com.mvlog.agent.api.usecase.ObserveToolApprovalRulesUseCase
import com.mvlog.agent.api.usecase.RetryChatUseCase
import com.mvlog.agent.api.usecase.RevokeToolApprovalRuleUseCase
import com.mvlog.agent.impl.domain.approval.ToolApprovalDecisions
import com.mvlog.agent.impl.domain.approval.ToolApprovalResolver
import com.mvlog.agent.impl.domain.approval.ToolSpecCatalog
import com.mvlog.agent.impl.domain.approval.gatedToolNames
import com.mvlog.agent.impl.domain.repository.ConversationRepository
import com.mvlog.agent.impl.domain.repository.ToolApprovalRuleRepository
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.agent.impl.util.IdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

internal class DecideToolCallUseCaseImpl(
    private val conversationRepository: ConversationRepository,
    private val rules: ToolApprovalRuleRepository,
    private val decisions: ToolApprovalDecisions,
    private val approvals: ToolApprovalResolver,
    private val retryChat: RetryChatUseCase,
    private val idGenerator: IdGenerator,
    private val clock: AgentClock,
) : DecideToolCallUseCase {

    override suspend fun invoke(chatId: ChatId, toolCallKey: String, decision: ToolApprovalDecision) {
        val call = conversationRepository.pendingToolCalls(chatId)
            .firstOrNull { it.key == toolCallKey }
            ?: return

        if (decision is ToolApprovalDecision.Approve && decision.scope != ToolApprovalScope.Once) {
            val scope = chatId.takeIf { decision.scope == ToolApprovalScope.Chat }
            val parameters = approvals.ruleParameters(call)
            val exists = rules.applicable(chatId).any {
                it.toolName == call.name && it.chatId == scope && it.parameters == parameters
            }
            if (!exists) {
                rules.add(
                    ToolApprovalRule(
                        id = ToolApprovalRuleId(idGenerator.newId()),
                        toolName = call.name,
                        chatId = scope,
                        parameters = parameters,
                        createdAt = clock.now(),
                    )
                )
            }
        }

        decisions.record(chatId, toolCallKey, decision)
        retryChat(chatId)
    }
}

internal class AddToolApprovalRuleUseCaseImpl(
    private val rules: ToolApprovalRuleRepository,
    private val conversationRepository: ConversationRepository,
    private val retryChat: RetryChatUseCase,
    private val idGenerator: IdGenerator,
    private val clock: AgentClock,
) : AddToolApprovalRuleUseCase {

    override suspend fun invoke(toolName: String, chatId: ChatId?) {
        val exists = rules.observeScoped(chatId).first()
            .any { it.toolName == toolName && it.parameters.isEmpty() }
        if (!exists) {
            rules.add(
                ToolApprovalRule(
                    id = ToolApprovalRuleId(idGenerator.newId()),
                    toolName = toolName,
                    chatId = chatId,
                    parameters = emptyMap(),
                    createdAt = clock.now(),
                )
            )
        }

        // A turn that was only waiting on this tool resumes; retry ignores anything else.
        val affected = chatId?.let(::listOf) ?: conversationRepository.chatsWithUnansweredPrompts()
        affected.forEach { retryChat(it) }
    }
}

internal class RevokeToolApprovalRuleUseCaseImpl(
    private val rules: ToolApprovalRuleRepository,
) : RevokeToolApprovalRuleUseCase {

    override suspend fun invoke(ruleId: ToolApprovalRuleId) = rules.remove(ruleId)
}

internal class ObserveToolApprovalRulesUseCaseImpl(
    private val rules: ToolApprovalRuleRepository,
) : ObserveToolApprovalRulesUseCase {

    override fun invoke(chatId: ChatId?): Flow<List<ToolApprovalRule>> = rules.observeScoped(chatId)
}

internal class GetApprovalGatedToolsUseCaseImpl(
    private val catalog: ToolSpecCatalog,
) : GetApprovalGatedToolsUseCase {

    override fun invoke(): List<String> = catalog.gatedToolNames().distinct()
}
