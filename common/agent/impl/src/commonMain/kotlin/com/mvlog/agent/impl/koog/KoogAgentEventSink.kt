package com.mvlog.agent.impl.koog

import ai.koog.prompt.streaming.StreamFrame
import com.mvlog.agent.impl.domain.entity.ToolCallStatus
import com.mvlog.agent.impl.domain.repository.ChatRepository
import com.mvlog.agent.impl.domain.repository.StreamingEntryKind
import com.mvlog.agent.impl.execution.AgentExecutionContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Deltas are buffered and flushed on flushIntervalMillis; a write per token would swamp storage and
 * the UI.
 */
@OptIn(ExperimentalTime::class)
internal class KoogAgentEventSink(
    private val chatRepository: ChatRepository,
    private val context: AgentExecutionContext,
    private val flushIntervalMillis: Long = DEFAULT_FLUSH_INTERVAL_MILLIS,
) {

    private val mutex = Mutex()

    private var assistant: StreamBuffer? = null

    private var reasoning: StreamBuffer? = null

    suspend fun onFrame(frame: StreamFrame) {
        when (frame) {
            is StreamFrame.TextDelta -> appendText(frame.text)
            is StreamFrame.TextComplete -> completeText()
            // text is null for summary-only or encrypted reasoning frames.
            is StreamFrame.ReasoningDelta -> frame.text?.let { appendReasoning(it) }
            is StreamFrame.ReasoningComplete -> completeReasoning()
            else -> Unit
        }
    }

    suspend fun onToolCallStarting(toolCallId: String?, name: String, arguments: String?) {
        // Flush first so the tool call lands after the text that introduced it.
        flushAll()
        // A call hydrated from a turn that stopped for approval already has its entry.
        val entryId = toolCallId?.let { chatRepository.findOpenToolCallEntryId(context.chatId, it) }
            ?: chatRepository.addToolCall(
                chatId = context.chatId,
                runId = context.runId,
                toolCallId = toolCallId,
                name = name,
                arguments = arguments,
            )
        chatRepository.updateToolCallStatus(entryId, ToolCallStatus.Running)
    }

    suspend fun onToolCallCompleted(toolCallId: String?, result: String) {
        updateToolCall(toolCallId, ToolCallStatus.Completed(result))
    }

    suspend fun onToolCallFailed(toolCallId: String?, error: String) {
        updateToolCall(toolCallId, ToolCallStatus.Failed(error))
    }

    suspend fun finish() {
        completeText()
        completeReasoning()
    }

    private suspend fun updateToolCall(toolCallId: String?, status: ToolCallStatus) {
        val entryId = toolCallId?.let {
            chatRepository.findToolCallEntryId(context.chatId, it)
        } ?: return
        chatRepository.updateToolCallStatus(entryId, status)
    }

    private suspend fun appendText(delta: String) = mutex.withLock {
        val buffer = assistant ?: newBuffer(StreamingEntryKind.Assistant).also { assistant = it }
        buffer.text.append(delta)
        buffer.flushIfDue()
    }

    private suspend fun appendReasoning(delta: String) = mutex.withLock {
        val buffer = reasoning ?: newBuffer(StreamingEntryKind.Reasoning).also { reasoning = it }
        buffer.text.append(delta)
        buffer.flushIfDue()
    }

    private suspend fun completeText() = mutex.withLock {
        assistant?.complete()
        assistant = null
    }

    private suspend fun completeReasoning() = mutex.withLock {
        reasoning?.complete()
        reasoning = null
    }

    private suspend fun flushAll() = mutex.withLock {
        assistant?.flush()
        reasoning?.flush()
    }

    private suspend fun newBuffer(kind: StreamingEntryKind): StreamBuffer =
        StreamBuffer(
            entryId = chatRepository.beginStreamingEntry(
                chatId = context.chatId,
                runId = context.runId,
                kind = kind,
            )
        )

    private inner class StreamBuffer(val entryId: String) {

        val text = StringBuilder()

        private var lastFlushAt = Clock.System.now().toEpochMilliseconds()

        suspend fun flushIfDue() {
            val now = Clock.System.now().toEpochMilliseconds()
            if (now - lastFlushAt < flushIntervalMillis) return
            lastFlushAt = now
            flush()
        }

        suspend fun flush() {
            chatRepository.updateStreamingText(entryId, text.toString())
        }

        suspend fun complete() {
            chatRepository.completeStreamingEntry(entryId, text.toString())
        }
    }

    private companion object {
        const val DEFAULT_FLUSH_INTERVAL_MILLIS = 60L
    }
}
