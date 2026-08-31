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

/**
 * Serves one run against a configured model.
 *
 * Restoration and durability are the framework's: chat memory loads the conversation at the start
 * and commits it at the end, and persistence checkpoints partial work as the run proceeds. Both
 * reach the same storage through the providers passed in here, keyed by chat id.
 *
 * The streaming nodes deliberately collect frames *inside* the LLM session rather than returning a
 * flow for someone else to collect: the session is alive there, ordering is guaranteed, and the
 * assembled reply can be appended to the prompt before the run ends — which is what chat memory
 * later commits.
 */
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
            // Per run, not per runner: a tool is scoped to one conversation, and the chat id only
            // exists once a run starts.
            .toolRegistry(toolRegistryFactory.create(context.chatId.value))
            // Strategy first: installing a feature beforehand pins the agent's types to
            // <String, String> and the mismatch surfaces somewhere unrelated.
            .graphStrategy(respondStrategy(sink))
            .install(ChatMemory.Feature) { config ->
                config.chatHistoryProvider = historyProvider
            }
            .install(Persistence.Feature) { config ->
                config.storage = persistenceStorage
                config.enableAutomaticPersistence = true
            }
            // The sink has always had these three methods and nothing called them; this is what
            // puts a tool call on the timeline as it happens rather than only once it is replayed.
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
                    // `message` rather than the throwable: it is the text the model will read as
                    // the result, and a stack trace tells it nothing it can act on.
                    sink.onToolCallFailed(
                        toolCallId = event.toolCallId,
                        error = event.message,
                    )
                }
            }
            .build()

        try {
            // The framework's session id is our chat id — the key both features store under.
            agent.run(context.prompt, context.chatId.value)
        } finally {
            agent.close()
            sink.finish()
        }
    }

    /**
     * The agentic loop: reason, call tools, read the results, reason again, then answer.
     *
     * One conversation re-sent in full each time round, not a pipeline with a planning stage — the
     * model's reasoning and its tool calls arrive in the *same* assistant turn, so there is no
     * planner node to write. The loop ends when a turn carries no tool calls.
     *
     * Nodes yield the whole [Message.Assistant], not its text. A reply can be reasoning plus a tool
     * call with no prose at all, so collapsing to a string at the graph boundary would discard
     * exactly what the branching below needs.
     */
    private fun respondStrategy(sink: KoogAgentEventSink) =
        strategy<String, Message.Assistant>(STRATEGY) {
            // Per run: `respondStrategy` is called once per `run()`, so this counts one
            // conversation's tool rounds and nothing else.
            var toolRounds = 0

            val respond by node<String, Message.Assistant>(NODE_RESPOND) { userText ->
                llm.writeSession {
                    // Per-protocol prompt parameters, such as Ollama's `think`. The runner stays
                    // protocol-agnostic: the client factory decides, this only applies.
                    target.params?.let(::changeLLMParams)

                    // Blank means a resumed turn: the conversation already ends with the tool
                    // results the model was interrupted before reading, so appending anything here
                    // would put a phantom user message in the transcript.
                    if (userText.isNotBlank()) {
                        appendPrompt { user(userText) }
                    }

                    streamReply(sink)
                }
            }

            val executeTools by nodeExecuteTools(NODE_EXECUTE_TOOLS, parallel = true)

            // Hand-written rather than Koog's nodeLLMSendToolResultsStreaming, which returns a Flow
            // for someone else to collect — the frames would then never reach the sink and every
            // turn after the first would stream nothing to the screen. It also cannot re-enter
            // `respond`: that node appends the user's prompt, which would re-ask the original
            // question on every iteration.
            val sendToolResults by node<ReceivedToolResults, Message.Assistant>(NODE_SEND_RESULTS) { results ->
                toolRounds++
                llm.writeSession {
                    appendPrompt {
                        user {
                            results.toolResults.forEach { result -> toolResult(result.toMessagePart()) }
                        }
                    }

                    if (toolRounds >= MAX_TOOL_ROUNDS) {
                        // Cutting the run off here would leave a turn that just stops. Saying so
                        // gets an answer built from whatever the model already gathered.
                        appendPrompt { user(TOOL_BUDGET_EXHAUSTED) }
                    }

                    streamReply(sink)
                }
            }

            nodeStart then respond

            // Order matters: `AIAgentNode.resolveEdge` takes the *first* edge that accepts the
            // output, and an assistant turn very often carries both prose and a tool call ("Let me
            // check that file." + read_file). With the finishing edge first, such a turn would end
            // the run and the tool would never execute — which reads as the model ignoring its
            // tools.
            //
            // The budget lives in this predicate rather than only in the nudge above, because a
            // message alone cannot stop the loop: a model that keeps asking for tools would keep
            // matching this edge. Once spent, the edge stops accepting and the run falls through to
            // the catch-all below — which is the only thing that actually bounds the loop.
            edge(respond forwardTo executeTools onToolCalls { toolRounds < MAX_TOOL_ROUNDS })

            // Deliberately unconditional: a reply with no tool calls, and a reply whose tool calls
            // are past the budget, both end the turn. Leaving a gap here would strand the run on a
            // node with no outgoing edge.
            edge(respond forwardTo nodeFinish)

            edge(executeTools forwardTo sendToolResults)

            edge(sendToolResults forwardTo executeTools onToolCalls { toolRounds < MAX_TOOL_ROUNDS })
            edge(sendToolResults forwardTo nodeFinish)
        }

    /**
     * Streams one assistant turn, recording every frame as it arrives, and appends the result.
     *
     * `requestLLMStreaming` leaves the prompt untouched, unlike `requestLLM` — so the reply is
     * appended here, and only after the stream completed. A run that dies mid-stream therefore
     * leaves no half-formed assistant message behind.
     */
    private suspend fun ai.koog.agents.core.agent.session.AIAgentLLMWriteSession.streamReply(
        sink: KoogAgentEventSink,
    ): Message.Assistant {
        val frames = mutableListOf<StreamFrame>()
        requestLLMStreaming().collect { frame ->
            frames += frame
            // Suspends until the frame is durably recorded. Deliberately not guarded: an agent that
            // keeps running when its transcript can no longer be written would finish with storage
            // missing half of what the user saw.
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

        /** A model that keeps calling tools would otherwise run until the user cancels it. */
        const val MAX_TOOL_ROUNDS = 12

        const val TOOL_BUDGET_EXHAUSTED =
            "You have reached the limit on tool calls for this turn. " +
                "Answer now using what you have already gathered."
    }
}
