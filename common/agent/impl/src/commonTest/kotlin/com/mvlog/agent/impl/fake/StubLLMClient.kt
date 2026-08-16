package com.mvlog.agent.impl.fake

import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.ModerationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * An LLM that answers from a script instead of a network.
 *
 * Lets a test drive the real agent — real graph, real features, real storage — and assert what the
 * framework does around it: when checkpoints are written, when history is committed, what survives
 * a failure. The only thing replaced is the model itself.
 *
 * [prompts] records what the model was actually asked, which is how a test proves that a restored
 * conversation reached it.
 */
@OptIn(ExperimentalTime::class)
internal class StubLLMClient(
    private val script: List<StreamFrame>,
    /** Thrown after emitting [failAfterFrames] frames, to simulate a run dying mid-stream. */
    private val failAfterFrames: Int? = null,
) : LLMClient() {

    val prompts = mutableListOf<Prompt>()

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Flow<StreamFrame> = flow {
        prompts += prompt
        script.forEachIndexed { index, frame ->
            if (failAfterFrames != null && index >= failAfterFrames) {
                error("stub model failed mid-stream")
            }
            emit(frame)
        }
    }

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Message.Assistant {
        prompts += prompt
        return Message.Assistant(
            parts = listOf(MessagePart.Text(script.filterIsInstance<StreamFrame.TextDelta>()
                .joinToString("") { it.text })),
            metaInfo = ResponseMetaInfo(timestamp = Clock.System.now()),
        )
    }

    override suspend fun models(): List<LLModel> = listOf(MODEL)

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        error("moderation is not part of what these tests exercise")

    override fun llmProvider(): LLMProvider = LLMProvider.OpenAI

    override fun close() = Unit

    companion object {
        val MODEL = LLModel(
            provider = LLMProvider.OpenAI,
            id = "stub-model",
            capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
        )

        fun executor(client: LLMClient): PromptExecutor = MultiLLMPromptExecutor(client)

        @OptIn(ExperimentalTime::class)
        fun textReply(vararg chunks: String): List<StreamFrame> =
            chunks.map { StreamFrame.TextDelta(it) } +
                StreamFrame.TextComplete(chunks.joinToString("")) +
                StreamFrame.End(finishReason = "stop", metaInfo = ResponseMetaInfo(Clock.System.now()))

        @OptIn(ExperimentalTime::class)
        fun replyWithToolCall(
            text: String,
            toolName: String,
            toolArgs: String,
        ): List<StreamFrame> = listOf(
            StreamFrame.TextDelta(text),
            StreamFrame.ToolCallComplete(id = "call-1", name = toolName, content = toolArgs),
            StreamFrame.End(finishReason = "tool_calls", metaInfo = ResponseMetaInfo(Clock.System.now())),
        )
    }
}
