package com.mvlog.agent.impl.koog

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.context.AIAgentGraphContextBase
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.ReceivedToolResults
import ai.koog.agents.core.dsl.extension.ToolCalls
import ai.koog.agents.core.dsl.extension.nodeExecuteTools
import ai.koog.agents.core.dsl.extension.onToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.environment.ToolResultKind
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.serialization.JSONObject
import ai.koog.serialization.kotlinx.toKoogJSONObject
import com.mvlog.agent.api.model.ChatId
import com.mvlog.agent.impl.domain.approval.ToolApprovalResolver
import com.mvlog.agent.impl.domain.approval.ToolCallVerdict
import com.mvlog.agent.impl.domain.approval.executes
import com.mvlog.agent.impl.domain.approval.isSettled
import com.mvlog.agent.impl.domain.approval.toolCallKey
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.domain.repository.ConversationRepository
import com.mvlog.agent.impl.execution.AgentExecutionContext
import com.mvlog.agent.impl.execution.AgentRunner
import com.mvlog.agent.impl.util.AgentClock
import com.mvlog.log.TLogger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal class KoogAgentRunner(
    private val target: KoogTarget,
    private val chatRepository: ChatRepository,
    private val conversationRepository: ConversationRepository,
    private val historyProvider: PersistentChatHistoryProvider,
    private val persistenceStorage: RoomPersistenceStorageProvider,
    private val toolRegistryFactory: KoogToolRegistryFactory,
    private val approvals: ToolApprovalResolver,
    private val clock: AgentClock,
) : AgentRunner {

    override suspend fun run(context: AgentExecutionContext) {
        val sink = KoogAgentEventSink(chatRepository = chatRepository, context = context)

        val agent = AIAgent.builder()
            .promptExecutor(target.executor)
            .llmModel(target.model)
            .systemPrompt(AGENT_SYSTEM_PROMPT)
            .toolRegistry(toolRegistryFactory.create(context.chatId.value))
            // graphStrategy before any install: a feature installed first pins the builder to
            // <String, String>.
            .graphStrategy(respondStrategy(sink, context.chatId))
            .install(ChatMemory.Feature) { config ->
                config.chatHistoryProvider = historyProvider
            }
            .install(Persistence.Feature) { config ->
                config.storage = persistenceStorage
                config.enableAutomaticPersistence = true
            }
            // Live tool-call entries: without this a call shows only once the run is replayed.
            .install(EventHandler.Feature) { config ->
                config.onToolCallStarting { event ->
                    sink.onToolCallStarting(
                        toolCallId = event.toolCallId,
                        name = event.toolName,
                        arguments = event.toolArgs.toString(),
                    )
                }
                config.onToolCallCompleted { event ->
                    sink.onToolCallCompleted(
                        toolCallId = event.toolCallId,
                        result = event.toolResult?.toString().orEmpty(),
                    )
                }
                config.onToolCallFailed { event ->
                    sink.onToolCallFailed(
                        toolCallId = event.toolCallId,
                        error = event.message,
                    )
                }
            }
            .build()

        try {
            agent.run(context.prompt, context.chatId.value)
        } finally {
            agent.close()
            sink.finish()
            refreshTimeline(context.chatId)
        }
    }

    private suspend fun refreshTimeline(chatId: ChatId) = withContext(NonCancellable) {
        runCatching { chatRepository.hydrate(chatId, conversationRepository.timeline(chatId)) }
            .onFailure { TLogger.e(TAG, "Could not refresh the timeline of ${chatId.value}", it) }
    }

    /**
     * Nodes yield Message.Assistant, not text: a turn may be reasoning plus a tool call with no
     * prose.
     */
    private fun respondStrategy(sink: KoogAgentEventSink, chatId: ChatId) =
        strategy<String, Message.Assistant>(STRATEGY) {
            var toolRounds = 0

            // Decided on the edges out of nodeStart, not in a node: a node here would be
            // checkpointed with a history the prompt has not been appended to yet.
            var resume: ResumeStep? = null
            suspend fun AIAgentGraphContextBase.resumeStep(userText: String): ResumeStep =
                resume ?: resumeStep(chatId, userText).also { resume = it }

            val respond by node<String, Message.Assistant>(NODE_RESPOND) { userText ->
                llm.writeSession {
                    target.params?.let(::changeLLMParams)

                    if (userText.isNotBlank()) {
                        appendPrompt { user(userText) }
                    }

                    streamReply(sink)
                }
            }

            val executeTools by nodeExecuteTools(NODE_EXECUTE_TOOLS, parallel = true)

            val executeDecided by node<ResumeStep.Execute, ReceivedToolResults>(NODE_EXECUTE_DECIDED) { step ->
                ReceivedToolResults(executeDecided(step, sink))
            }

            val sendToolResults by node<ReceivedToolResults, Message.Assistant>(NODE_SEND_RESULTS) { results ->
                toolRounds++
                llm.writeSession {
                    appendPrompt {
                        user {
                            results.toolResults.forEach { result -> toolResult(result.toMessagePart()) }
                        }
                    }

                    if (toolRounds >= MAX_TOOL_ROUNDS) {
                        // Tell the model the budget is spent so the turn ends with an answer, not
                        // mid-loop.
                        appendPrompt { user(TOOL_BUDGET_EXHAUSTED) }
                    }

                    streamReply(sink)
                }
            }

            edge(
                nodeStart forwardTo executeDecided
                    onCondition { resumeStep(it) is ResumeStep.Execute }
                    transformed { resumeStep(it) as ResumeStep.Execute }
            )
            edge(
                nodeStart forwardTo nodeFinish
                    onCondition { resumeStep(it) is ResumeStep.StillWaiting }
                    transformed { (resumeStep(it) as ResumeStep.StillWaiting).message }
            )
            edge(nodeStart forwardTo respond)

            // Tool edge first: resolveEdge takes the first accepting edge.
            // The predicate, not the nudge, bounds the loop; a model can ignore a message.
            // A call still waiting on the user falls through to finish, which ends the run.
            edge(
                respond forwardTo executeTools
                    onToolCalls { toolRounds < MAX_TOOL_ROUNDS }
                    onCondition { mayExecute(chatId, it) }
            )

            // Unconditional: also the exit once the tool budget is spent.
            edge(respond forwardTo nodeFinish)

            edge(executeTools forwardTo sendToolResults)
            edge(executeDecided forwardTo sendToolResults)

            edge(
                sendToolResults forwardTo executeTools
                    onToolCalls { toolRounds < MAX_TOOL_ROUNDS }
                    onCondition { mayExecute(chatId, it) }
            )
            edge(sendToolResults forwardTo nodeFinish)
        }

    private suspend fun AIAgentGraphContextBase.resumeStep(chatId: ChatId, userText: String): ResumeStep {
        val last = llm.readSession { prompt.messages.lastOrNull() }
        val calls = last.trailingToolCalls()
        if (userText.isNotBlank() || calls.isEmpty()) return ResumeStep.Prompt

        val verdicts = approvals.verdicts(chatId, calls.toPendingToolCalls())
        return if (verdicts.values.all { it.isSettled }) {
            ResumeStep.Execute(
                calls = calls,
                declinedKeys = verdicts.filterValues { it is ToolCallVerdict.Declined }.keys,
            )
        } else {
            ResumeStep.StillWaiting(last as Message.Assistant)
        }
    }

    private suspend fun mayExecute(chatId: ChatId, calls: ToolCalls): Boolean =
        approvals.verdicts(chatId, calls.toolCalls.toPendingToolCalls()).values.all { it.executes }

    private suspend fun AIAgentGraphContextBase.executeDecided(
        step: ResumeStep.Execute,
        sink: KoogAgentEventSink,
    ): List<ReceivedToolResult> {
        val declined = step.calls.indices
            .filter { ordinal -> toolCallKey(step.calls[ordinal].id, ordinal) in step.declinedKeys }
            .toSet()
        val executed = environment.executeTools(
            step.calls.filterIndexed { ordinal, _ -> ordinal !in declined }
        ).iterator()

        return step.calls.mapIndexed { ordinal, call ->
            if (ordinal in declined) {
                sink.onToolCallFailed(call.id, DECLINED_TEXT)
                call.declinedResult()
            } else {
                executed.next()
            }
        }
    }

    private fun MessagePart.Tool.Call.declinedResult() = ReceivedToolResult(
        id = id,
        tool = tool,
        toolArgs = runCatching { argsJson.toKoogJSONObject() }.getOrElse { JSONObject(emptyMap()) },
        toolDescription = null,
        output = DECLINED_TEXT,
        resultKind = ToolResultKind.Failure(null),
        result = null,
    )

    /** requestLLMStreaming does not append the reply to the prompt; requestLLM does. */
    private suspend fun ai.koog.agents.core.agent.session.AIAgentLLMWriteSession.streamReply(
        sink: KoogAgentEventSink,
    ): Message.Assistant {
        val frames = mutableListOf<StreamFrame>()
        requestLLMStreaming().collect { frame ->
            frames += frame
            // Unguarded on purpose: if the timeline cannot be written the run should fail, not
            // finish half done.
            sink.onFrame(frame)
        }

        // Qualified: an LLM session exposes its own `clock`, which would shadow ours.
        val response = frames.toMessageResponse(this@KoogAgentRunner.clock)
        appendPrompt { message(response) }
        return response
    }

    private sealed interface ResumeStep {

        data object Prompt : ResumeStep

        data class Execute(
            val calls: List<MessagePart.Tool.Call>,
            val declinedKeys: Set<String>,
        ) : ResumeStep

        data class StillWaiting(val message: Message.Assistant) : ResumeStep
    }

    private companion object {
        const val TAG = "KoogAgentRunner"
        const val STRATEGY = "thoon-chat"
        const val NODE_RESPOND = "respond"
        const val NODE_EXECUTE_TOOLS = "execute-tools"
        const val NODE_EXECUTE_DECIDED = "execute-decided"
        const val NODE_SEND_RESULTS = "send-tool-results"

        const val MAX_TOOL_ROUNDS = 12

        const val DECLINED_TEXT = "Declined by the user."

        const val TOOL_BUDGET_EXHAUSTED =
            "You have reached the limit on tool calls for this turn. " +
                "Answer now using what you have already gathered."
    }
}
