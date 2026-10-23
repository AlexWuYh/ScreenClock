package app.screenclock.domain.occasion

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class CeremonyMode {
    Counting,
    Done,
}

/** Counting until the morning of the date itself; from then on the message takes over. */
fun ceremonyMode(now: LocalDateTime, occurrence: LocalDate): CeremonyMode {
    return if (now.isBefore(occurrence.atStartOfDay())) CeremonyMode.Counting else CeremonyMode.Done
}

/** What is left, split into the four blocks of the page. */
data class CeremonyParts(
    val days: Long,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
)

/** Seconds round up, so the page shows 00:00:01 until the very last second and never 0 early. */
fun ceremonyParts(now: LocalDateTime, occurrence: LocalDate): CeremonyParts {
    val remaining = Duration.between(now, occurrence.atStartOfDay()).toMillis().coerceAtLeast(0L)
    val total = (remaining + 999L) / 1000L
    return CeremonyParts(
        days = total / 86_400L,
        hours = ((total % 86_400L) / 3_600L).toInt(),
        minutes = ((total % 3_600L) / 60L).toInt(),
        seconds = (total % 60L).toInt(),
    )
}

/** Two digits for every block; the day count grows to as many digits as it needs. */
fun twoDigits(value: Number): String = value.toLong().coerceAtLeast(0L).toString().padStart(2, '0')

/** The wait until the displayed second changes. Done does not tick. */
fun millisUntilCeremonyTick(now: LocalDateTime, occurrence: LocalDate): Long? {
    val start = occurrence.atStartOfDay()
    if (!now.isBefore(start)) return null
    val remaining = Duration.between(now, start).toMillis()
    val into = remaining % 1000L
    return if (into == 0L) 1000L else into
}

data class CeremonyFace(
    val title: String,
    val icon: OccasionIcon,
    val solarLabel: String,
    val message: String,
    val mode: CeremonyMode,
    val parts: CeremonyParts,
    val spoken: String,
)

fun ceremonyFace(
    title: String,
    icon: OccasionIcon,
    message: String,
    solarLabel: String,
    now: LocalDateTime,
    occurrence: LocalDate,
): CeremonyFace {
    val mode = ceremonyMode(now, occurrence)
    val parts = if (mode == CeremonyMode.Counting) {
        ceremonyParts(now, occurrence)
    } else {
        CeremonyParts(0, 0, 0, 0)
    }
    return CeremonyFace(
        title = title,
        icon = icon,
        solarLabel = solarLabel,
        message = message,
        mode = mode,
        parts = parts,
        spoken = ceremonySpoken(title, mode, parts, message),
    )
}

fun ceremonySpoken(title: String, mode: CeremonyMode, parts: CeremonyParts, message: String): String {
    return when (mode) {
        CeremonyMode.Counting ->
            "${title}，还有${parts.days}天${parts.hours}小时${parts.minutes}分${parts.seconds}秒"
        CeremonyMode.Done -> message
    }
}
