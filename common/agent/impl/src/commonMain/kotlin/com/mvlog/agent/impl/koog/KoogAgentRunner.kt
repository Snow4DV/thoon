package com.mvlog.agent.impl.koog

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.ReceivedToolResults
import ai.koog.agents.core.dsl.extension.nodeExecuteTools
import ai.koog.agents.core.dsl.extension.onToolCalls
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.snapshot.feature.Persistence
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.execution.AgentExecutionContext
import com.mvlog.agent.impl.execution.AgentRunner
import com.mvlog.agent.impl.util.AgentClock

internal class KoogAgentRunner(
    private val target: KoogTarget,
    private val chatRepository: ChatRepository,
    private val historyProvider: PersistentChatHistoryProvider,
    private val persistenceStorage: RoomPersistenceStorageProvider,
    private val toolRegistryFactory: KoogToolRegistryFactory,
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
            .graphStrategy(respondStrategy(sink))
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
        }
    }

    /**
     * Nodes yield Message.Assistant, not text: a turn may be reasoning plus a tool call with no
     * prose.
     */
    private fun respondStrategy(sink: KoogAgentEventSink) =
        strategy<String, Message.Assistant>(STRATEGY) {
            var toolRounds = 0

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

            nodeStart then respond

            // Tool edge first: resolveEdge takes the first accepting edge.
            // The predicate, not the nudge, bounds the loop; a model can ignore a message.
            edge(respond forwardTo executeTools onToolCalls { toolRounds < MAX_TOOL_ROUNDS })

            // Unconditional: also the exit once the tool budget is spent.
            edge(respond forwardTo nodeFinish)

            edge(executeTools forwardTo sendToolResults)

            edge(sendToolResults forwardTo executeTools onToolCalls { toolRounds < MAX_TOOL_ROUNDS })
            edge(sendToolResults forwardTo nodeFinish)
        }

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

    private companion object {
        const val STRATEGY = "thoon-chat"
        const val NODE_RESPOND = "respond"
        const val NODE_EXECUTE_TOOLS = "execute-tools"
        const val NODE_SEND_RESULTS = "send-tool-results"

        const val MAX_TOOL_ROUNDS = 12

        const val TOOL_BUDGET_EXHAUSTED =
            "You have reached the limit on tool calls for this turn. " +
                "Answer now using what you have already gathered."
    }
}
