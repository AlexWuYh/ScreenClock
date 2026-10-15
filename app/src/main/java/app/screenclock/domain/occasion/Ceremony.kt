package app.screenclock.domain.occasion

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val DAY_MILLIS: Long = 24L * 3_600_000L

enum class CeremonyMode {
    Days,
    Clock,
    Done,
}

/** Days while a full 24 hours remain. The morning of the date itself is already done. */
fun ceremonyMode(now: LocalDateTime, occurrence: LocalDate): CeremonyMode {
    val start = occurrence.atStartOfDay()
    if (!now.isBefore(start)) return CeremonyMode.Done
    val remaining = Duration.between(now, start)
    return if (remaining >= Duration.ofHours(24)) CeremonyMode.Days else CeremonyMode.Clock
}

fun ceremonyDayCount(today: LocalDate, occurrence: LocalDate): Long {
    return ChronoUnit.DAYS.between(today, occurrence).coerceAtLeast(0L)
}

/**
 * 1–9 stay one digit. 10–99 stay one pair. 100 and above split into pairs from the right,
 * and the first group is not padded.
 */
fun dayGroups(days: Long): List<String> {
    val text = days.coerceAtLeast(0L).toString()
    if (text.length <= 2) return listOf(text)
    val groups = ArrayDeque<String>()
    var end = text.length
    while (end > 0) {
        val start = (end - 2).coerceAtLeast(0)
        groups.addFirst(text.substring(start, end))
        end = start
    }
    return groups.toList()
}

/**
 * Last 24 hours always use three pairs, including a leading 00 hour.
 * Seconds round up, but a remainder under 24 hours never displays 24:00:00.
 */
fun ceremonyClockGroups(now: LocalDateTime, occurrence: LocalDate): List<String> {
    val remaining = Duration.between(now, occurrence.atStartOfDay()).toMillis().coerceAtLeast(0L)
    val rounded = (remaining + 999L) / 1000L
    val totalSeconds = if (remaining < DAY_MILLIS) {
        rounded.coerceAtMost(DAY_MILLIS / 1000L - 1L)
    } else {
        rounded
    }
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return listOf(twoDigits(hours), twoDigits(minutes), twoDigits(seconds))
}

/**
 * Days mode waits until the next midnight, or until the last 24 hours begin.
 * An exact 24-hour remainder wakes 1 millisecond later so the clock can take over.
 * Clock mode waits until the rounded second changes. Done does not tick.
 */
fun millisUntilCeremonyTick(now: LocalDateTime, occurrence: LocalDate): Long? {
    val start = occurrence.atStartOfDay()
    if (!now.isBefore(start)) return null
    val remaining = Duration.between(now, start).toMillis()
    if (remaining < DAY_MILLIS) {
        val into = remaining % 1000L
        return if (into == 0L) 1000L else into
    }
    if (remaining == DAY_MILLIS) return 1L
    val untilMidnight = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()
    val untilClock = remaining - DAY_MILLIS
    return minOf(untilMidnight, untilClock).coerceAtLeast(1L)
}

data class CeremonyFace(
    val title: String,
    val solarLabel: String,
    val message: String,
    val mode: CeremonyMode,
    val groups: List<String>,
    val spoken: String,
)

fun ceremonyFace(
    title: String,
    message: String,
    solarLabel: String,
    now: LocalDateTime,
    occurrence: LocalDate,
): CeremonyFace {
    val mode = ceremonyMode(now, occurrence)
    val days = ceremonyDayCount(now.toLocalDate(), occurrence)
    val groups = when (mode) {
        CeremonyMode.Days -> dayGroups(days)
        CeremonyMode.Clock -> ceremonyClockGroups(now, occurrence)
        CeremonyMode.Done -> emptyList()
    }
    return CeremonyFace(
        title = title,
        solarLabel = solarLabel,
        message = message,
        mode = mode,
        groups = groups,
        spoken = ceremonySpoken(title, mode, days, groups, message),
    )
}

fun ceremonySpoken(
    title: String,
    mode: CeremonyMode,
    dayCount: Long,
    groups: List<String>,
    message: String,
): String {
    return when (mode) {
        CeremonyMode.Days -> "${title}，还有${dayCount}天"
        CeremonyMode.Clock -> {
            val hours = groups.getOrElse(0) { "0" }.toIntOrNull() ?: 0
            val minutes = groups.getOrElse(1) { "00" }
            val seconds = groups.getOrElse(2) { "00" }
            "${title}，剩余${hours}小时${minutes}分${seconds}秒"
        }
        CeremonyMode.Done -> message
    }
}

private fun twoDigits(value: Long): String {
    return value.toString().padStart(2, '0')
}
