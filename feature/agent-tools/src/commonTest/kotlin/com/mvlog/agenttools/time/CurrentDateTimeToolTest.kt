package com.mvlog.agenttools.time

import com.mvlog.agent.tool.ChatToolContext
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class CurrentDateTimeToolTest {

    @Test
    fun theOrdinaryCaseReadsAsThreeLines() = runTest {
        // 2026-09-16T11:32:07Z is 14:32 in Moscow.
        val result = report("2026-09-16T11:32:07Z", "Europe/Moscow")

        assertEquals(
            """
            2026-09-16 (Wednesday)
            14:32 Europe/Moscow (UTC+03:00)
            ISO-8601: 2026-09-16T14:32:07+03:00
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun theLocalDateWinsOverTheUtcDate() = runTest {
        // 22:10Z on the 15th is 01:10 on the 16th in Moscow.
        val result = report("2026-09-15T22:10:00Z", "Europe/Moscow")

        assertEquals("2026-09-16 (Wednesday)", result.lineSequence().first())
    }

    @Test
    fun ahalfHourOffsetIsRenderedInFull() = runTest {
        val result = report("2026-09-16T09:02:07Z", "Asia/Kolkata")

        assertEquals("14:32 Asia/Kolkata (UTC+05:30)", result.lineSequence().elementAt(1))
        assertEquals("ISO-8601: 2026-09-16T14:32:07+05:30", result.lineSequence().last())
    }

    @Test
    fun utcSpellsItsOffsetTwoWaysOnPurpose() = runTest {
        val result = report("2026-09-16T14:32:07Z", "UTC")

        assertEquals("14:32 UTC (UTC+00:00)", result.lineSequence().elementAt(1))
        assertEquals("ISO-8601: 2026-09-16T14:32:07Z", result.lineSequence().last())
    }

    @Test
    fun midnightKeepsItsShape() = runTest {
        val result = report("2026-09-16T00:00:00Z", "UTC")

        assertEquals("00:00 UTC (UTC+00:00)", result.lineSequence().elementAt(1))
        assertEquals("ISO-8601: 2026-09-16T00:00:00Z", result.lineSequence().last())
    }

    private suspend fun report(instant: String, zone: String): String =
        CurrentDateTimeTool(
            now = { Instant.parse(instant) },
            zone = { TimeZone.of(zone) },
        ).execute(
            context = ChatToolContext(chatId = "chat"),
            arguments = JsonObject(emptyMap()),
        )
}
