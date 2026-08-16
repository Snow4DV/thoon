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
 * Translates one run's Koog events into timeline writes. No Koog type escapes this class.
 *
 * Text arrives as many small deltas, so they are accumulated in memory and flushed on a time
 * budget rather than written per delta — a write per token would swamp storage and the observing
 * UI. Tool events are rare and are persisted immediately.
 *
 * Instances are per run, so the buffered state here is never shared across runs.
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
            // Providers may send reasoning frames that only carry a summary or an encrypted blob.
            is StreamFrame.ReasoningDelta -> frame.text?.let { appendReasoning(it) }
            is StreamFrame.ReasoningComplete -> completeReasoning()
            else -> Unit
        }
    }

    suspend fun onToolCallStarting(toolCallId: String?, name: String, arguments: String?) {
        // Flush first so the tool call lands after the text that introduced it.
        flushAll()
        val entryId = chatRepository.addToolCall(
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

    /** Writes out whatever is buffered and closes any open entry. */
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
