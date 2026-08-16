package com.mvlog.agent.impl.koog

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
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
 * The streaming node deliberately collects frames *inside* the LLM session rather than returning a
 * flow for someone else to collect: the session is alive there, ordering is guaranteed, and the
 * assembled reply can be appended to the prompt before the run ends — which is what chat memory
 * later commits.
 */
internal class KoogAgentRunner(
    private val target: KoogTarget,
    private val chatRepository: ChatRepository,
    private val historyProvider: PersistentChatHistoryProvider,
    private val persistenceStorage: RoomPersistenceStorageProvider,
    private val clock: AgentClock,
) : AgentRunner {

    override suspend fun run(context: AgentExecutionContext) {
        val sink = KoogAgentEventSink(chatRepository = chatRepository, context = context)

        val agent = AIAgent.builder()
            .promptExecutor(target.executor)
            .llmModel(target.model)
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
     * The node yields the whole [Message.Assistant], not its text.
     *
     * A reply can be reasoning plus a tool call with no prose at all, so collapsing to a string at
     * the graph boundary would discard exactly what a tool-routing edge needs to branch on. Adding
     * that loop later is then an edge, not a rewrite of this node.
     */
    private fun respondStrategy(sink: KoogAgentEventSink) =
        strategy<String, Message.Assistant>(STRATEGY) {
            val respond by node<String, Message.Assistant>(NODE) { userText ->
                llm.writeSession {
                    appendPrompt { user(userText) }

                    val frames = mutableListOf<StreamFrame>()
                    requestLLMStreaming().collect { frame ->
                        frames += frame
                        // Suspends until the frame is durably recorded. Deliberately not guarded:
                        // an agent that keeps running when its transcript can no longer be written
                        // would finish with storage missing half of what the user saw.
                        sink.onFrame(frame)
                    }

                    // requestLLMStreaming leaves the prompt untouched, unlike requestLLM — so the
                    // reply is appended here, and only after the stream completed. A run that dies
                    // mid-stream therefore leaves no half-formed assistant message behind.
                    // Qualified: an LLM session exposes its own `clock`, which would shadow ours.
                    val response = frames.toMessageResponse(this@KoogAgentRunner.clock)
                    appendPrompt { message(response) }

                    response
                }
            }

            nodeStart then respond
            respond then nodeFinish
        }

    private companion object {
        const val STRATEGY = "thoon-chat"
        const val NODE = "respond"
    }
}
