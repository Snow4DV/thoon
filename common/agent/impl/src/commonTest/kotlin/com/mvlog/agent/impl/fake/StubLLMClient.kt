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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class StubLLMClient(
    /** One script per turn; the last one repeats, so a single-turn test needs no padding. */
    private vararg val scripts: List<StreamFrame>,
    private val failAfterFrames: Int? = null,
    /** Virtual-time pause before each frame, so a test can act mid-stream. */
    private val frameDelayMillis: Long = 0L,
) : LLMClient() {

    val prompts = mutableListOf<Prompt>()

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>,
    ): Flow<StreamFrame> = flow {
        val script = scripts[minOf(prompts.size, scripts.lastIndex)]
        prompts += prompt
        script.forEachIndexed { index, frame ->
            if (failAfterFrames != null && index >= failAfterFrames) {
                error("stub model failed mid-stream")
            }
            if (frameDelayMillis > 0) delay(frameDelayMillis)
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
            parts = listOf(MessagePart.Text(scripts.first().filterIsInstance<StreamFrame.TextDelta>()
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
