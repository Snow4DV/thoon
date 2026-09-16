package com.mvlog.agenttools.time

import com.mvlog.agent.tool.AgentToolSpec
import com.mvlog.agent.tool.ChatToolContext
import com.mvlog.agent.tool.ThoonAgentTool
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** [now] and [zone] are injectable so a test can pin the output. */
@OptIn(ExperimentalTime::class)
internal class CurrentDateTimeTool(
    private val now: () -> Instant = { Clock.System.now() },
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ThoonAgentTool {

    override val spec = AgentToolSpec(
        name = "current_datetime",
        description =
            "The current date, time and time zone on this device. Call this whenever an answer " +
                "depends on today's date, the time right now, or how far away something is — you " +
                "have no way to know any of those on your own.",
    )

    override suspend fun execute(context: ChatToolContext, arguments: JsonObject): String {
        val instant = now()
        val timeZone = zone()
        val local = instant.toLocalDateTime(timeZone)
        val offset = timeZone.offsetAt(instant)

        val date = local.date.format(LocalDate.Formats.ISO)

        return buildString {
            appendLine("$date (${local.date.dayOfWeek.readable()})")
            appendLine("${local.time.format(CLOCK_TIME)} ${timeZone.id} (UTC${offset.format(NUMERIC_OFFSET)})")
            // `Z` here (ISO-8601 spelling); `+00:00` above, where it follows the literal text
            // "UTC".
            append("ISO-8601: ${date}T${local.time.format(PRECISE_TIME)}${offset.format(UtcOffset.Formats.ISO)}")
        }
    }

    private fun DayOfWeek.readable(): String =
        name.lowercase().replaceFirstChar { it.uppercase() }

    private companion object {

        /** Explicit formats: `toString()` drops zero seconds, so the line's shape would vary. */
        val CLOCK_TIME = LocalTime.Format {
            hour()
            char(':')
            minute()
        }

        val PRECISE_TIME = LocalTime.Format {
            hour()
            char(':')
            minute()
            char(':')
            second()
        }

        val NUMERIC_OFFSET = UtcOffset.Format {
            offsetHours()
            char(':')
            offsetMinutesOfHour()
        }
    }
}
