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

/**
 * What day it is, where the user is.
 *
 * A model's sense of "now" is the day its training data ended, and it has no way to notice that it
 * is wrong — so "what is today's date", "how long until Friday" and "is this recent" are answered
 * confidently and incorrectly unless something tells it otherwise.
 *
 * This is a tool rather than a line in the system prompt because the prompt is written into a chat's
 * stored history on its first turn and replayed verbatim from then on: a date baked in there would
 * freeze at the moment the conversation started, and a chat resumed a week later would be misled
 * with exactly the same confidence. A tool is evaluated per call.
 *
 * [now] and [zone] are injected so the output can be pinned in a test. `AgentClock` would have been
 * the thing to reuse, but it is `internal` to `common:agent:impl`.
 */
@OptIn(ExperimentalTime::class)
internal class CurrentDateTimeTool(
    private val now: () -> Instant = { Clock.System.now() },
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) : ThoonAgentTool {

    override val spec = AgentToolSpec(
        name = "current_datetime",
        // The only thing that tells a model when to reach for this, so it names the cases rather
        // than describing the return value.
        description =
            "The current date, time and time zone on this device. Call this whenever an answer " +
                "depends on today's date, the time right now, or how far away something is — you " +
                "have no way to know any of those on your own.",
    )

    /** Takes no arguments; [arguments] is ignored. */
    override suspend fun execute(context: ChatToolContext, arguments: JsonObject): String {
        val instant = now()
        val timeZone = zone()
        val local = instant.toLocalDateTime(timeZone)
        val offset = timeZone.offsetAt(instant)

        val date = local.date.format(LocalDate.Formats.ISO)

        return buildString {
            appendLine("$date (${local.date.dayOfWeek.readable()})")
            appendLine("${local.time.format(CLOCK_TIME)} ${timeZone.id} (UTC${offset.format(NUMERIC_OFFSET)})")
            // The offset renders as `Z` here and as `+00:00` on the line above: this line is
            // ISO-8601, where `Z` is the spelling, and that one is for a human reading "UTC".
            append("ISO-8601: ${date}T${local.time.format(PRECISE_TIME)}${offset.format(UtcOffset.Formats.ISO)}")
        }
    }

    /** `WEDNESDAY` is how the enum spells it; nobody reads it that way. */
    private fun DayOfWeek.readable(): String =
        name.lowercase().replaceFirstChar { it.uppercase() }

    private companion object {

        /**
         * Formats are built rather than taken from `toString()`, which drops zero seconds — output
         * whose shape depends on the second it ran is not output a test can pin.
         */
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

        /** Always `+HH:MM`, never `Z` — it follows the literal text "UTC". */
        val NUMERIC_OFFSET = UtcOffset.Format {
            offsetHours()
            char(':')
            offsetMinutesOfHour()
        }
    }
}
