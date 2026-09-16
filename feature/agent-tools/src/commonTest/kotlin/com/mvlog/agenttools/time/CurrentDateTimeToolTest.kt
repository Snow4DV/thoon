package com.mvlog.agenttools.time

import com.mvlog.agent.tool.ChatToolContext
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * The output a model reads to learn what day it is.
 *
 * Pinned exactly rather than loosely, because every part of it is load-bearing: a model that gets
 * the zone wrong reasons about "today" wrong, and one handed a shape that shifts between calls has
 * no reason to trust any of it.
 */
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
        // Late evening in Moscow is still the previous day in UTC. Reporting the instant's date
        // would tell the model it is the 15th while the user's phone says the 16th — the whole
        // reason a time zone is read at all.
        val result = report("2026-09-15T22:10:00Z", "Europe/Moscow")

        assertEquals("2026-09-16 (Wednesday)", result.lineSequence().first())
    }

    @Test
    fun ahalfHourOffsetIsRenderedInFull() = runTest {
        // Catches an offset formatter that only renders hours — India is +05:30, not +05.
        val result = report("2026-09-16T09:02:07Z", "Asia/Kolkata")

        assertEquals("14:32 Asia/Kolkata (UTC+05:30)", result.lineSequence().elementAt(1))
        assertEquals("ISO-8601: 2026-09-16T14:32:07+05:30", result.lineSequence().last())
    }

    @Test
    fun utcSpellsItsOffsetTwoWaysOnPurpose() = runTest {
        // `Z` is the ISO spelling and belongs on the ISO line; the human line follows the literal
        // text "UTC", where a bare `Z` would read as "UTCZ".
        val result = report("2026-09-16T14:32:07Z", "UTC")

        assertEquals("14:32 UTC (UTC+00:00)", result.lineSequence().elementAt(1))
        assertEquals("ISO-8601: 2026-09-16T14:32:07Z", result.lineSequence().last())
    }

    @Test
    fun midnightKeepsItsShape() = runTest {
        // `toString()` would drop the zero seconds and shift the shape of the line under a model
        // that had learned to expect it.
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
