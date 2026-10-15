package app.screenclock.domain.occasion

import java.time.DateTimeException
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

fun nextOccurrence(today: LocalDate, item: Occasion, calendar: LunarCalendar): NextOccurrence {
    return when (item.kind) {
        OccasionKind.SolarOnce -> nextSolarOnce(today, item.year, item.month, item.day)
        OccasionKind.SolarYearly -> nextSolarYearly(today, item.month, item.day)
        OccasionKind.LunarYearly -> nextLunar(today, item.month, item.day, item.leap, calendar)
    }
}

fun nextOccurrence(today: LocalDate, draft: OccasionDraft, calendar: LunarCalendar): NextOccurrence {
    return when (draft.kind) {
        OccasionKind.SolarOnce -> nextSolarOnce(today, draft.year, draft.month, draft.day)
        OccasionKind.SolarYearly -> nextSolarYearly(today, draft.month, draft.day)
        OccasionKind.LunarYearly -> nextLunar(today, draft.month, draft.day, draft.leap, calendar)
    }
}

fun previewText(next: NextOccurrence): String {
    return when (next.status) {
        OccasionStatus.NotFound -> "近期没有这一天"
        OccasionStatus.Passed -> "已过"
        OccasionStatus.Today -> "就是今天"
        OccasionStatus.Future -> {
            val date = next.date ?: return "近期没有这一天"
            val shown = formatSolarDate(date)
            when {
                next.missingLeapThisYear -> "今年没有这个闰月，下次是 $shown"
                next.skipped -> "有的年份没有这一天，下次是 $shown"
                else -> "下次是 $shown"
            }
        }
    }
}

fun formatSolarDate(date: LocalDate): String {
    return "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
}

/**
 * Clock bar for the pinned item only.
 * A future date at least 24 hours away counts days. Under 24 hours counts whole hours, at least 1.
 * Today is 「今天 · 标题」. Passed and not-found leave the bar empty.
 */
fun occasionLine(item: Occasion, now: LocalDateTime, calendar: LunarCalendar): String? {
    if (!item.pinned) return null
    val next = nextOccurrence(now.toLocalDate(), item, calendar)
    val date = next.date
    return when (next.status) {
        OccasionStatus.Today -> "今天 · ${item.title}"
        OccasionStatus.Future -> {
            if (date == null) return null
            val remaining = Duration.between(now, date.atStartOfDay())
            if (remaining >= Duration.ofHours(24)) {
                val days = ChronoUnit.DAYS.between(now.toLocalDate(), date)
                "距${item.title} $days 天"
            } else {
                val hours = (remaining.toMillis() / 3_600_000L).coerceAtLeast(1L)
                "距${item.title} $hours 小时"
            }
        }
        OccasionStatus.Passed, OccasionStatus.NotFound -> null
    }
}

/** Solar once uses the real length of that month. A yearly February may be the 29th. Lunar days stop at 30. */
fun occasionDayMax(kind: OccasionKind, year: Int, month: Int): Int {
    if (month !in 1..12) return 30
    return when (kind) {
        OccasionKind.LunarYearly -> 30
        OccasionKind.SolarYearly -> LocalDate.of(2024, month, 1).lengthOfMonth()
        OccasionKind.SolarOnce -> try {
            LocalDate.of(year, month, 1).lengthOfMonth()
        } catch (_: DateTimeException) {
            31
        }
    }
}

fun pinnedOccasionLine(items: List<Occasion>, now: LocalDateTime, calendar: LunarCalendar): String? {
    val pinned = items.firstOrNull { it.pinned } ?: return null
    return occasionLine(pinned, now, calendar)
}

private fun nextSolarOnce(today: LocalDate, year: Int?, month: Int, day: Int): NextOccurrence {
    if (year == null) return NextOccurrence(OccasionStatus.NotFound)
    val date = solarOrNull(year, month, day) ?: return NextOccurrence(OccasionStatus.NotFound)
    val status = when {
        date > today -> OccasionStatus.Future
        date == today -> OccasionStatus.Today
        else -> OccasionStatus.Passed
    }
    return NextOccurrence(status, date)
}

private fun nextSolarYearly(today: LocalDate, month: Int, day: Int): NextOccurrence {
    if (month !in 1..12 || day !in 1..yearlyDayMax(month)) {
        return NextOccurrence(OccasionStatus.NotFound)
    }
    var skipped = false
    for (offset in 0 until OCCASION_SCAN_YEARS) {
        val date = solarOrNull(today.year + offset, month, day)
        if (date == null) {
            skipped = true
            continue
        }
        if (date < today) continue
        val status = if (date == today) OccasionStatus.Today else OccasionStatus.Future
        return NextOccurrence(status, date, skipped = skipped)
    }
    return NextOccurrence(OccasionStatus.NotFound, skipped = skipped)
}

private fun nextLunar(
    today: LocalDate,
    month: Int,
    day: Int,
    leap: Boolean,
    calendar: LunarCalendar,
): NextOccurrence {
    if (month !in 1..12 || day !in 1..30) return NextOccurrence(OccasionStatus.NotFound)
    if (leap && !calendar.leapMonthEver(month)) return NextOccurrence(OccasionStatus.NotFound)
    val startYear = calendar.lunarYear(today)
    val missingThisYear = leap && calendar.leapMonthOf(startYear) != month
    var skipped = false
    for (offset in 0 until OCCASION_SCAN_YEARS) {
        val date = calendar.solarOf(startYear + offset, month, day, leap)
        if (date == null) {
            skipped = true
            continue
        }
        if (date < today) continue
        val status = if (date == today) OccasionStatus.Today else OccasionStatus.Future
        return NextOccurrence(
            status = status,
            date = date,
            skipped = skipped,
            missingLeapThisYear = missingThisYear && status == OccasionStatus.Future,
        )
    }
    return NextOccurrence(OccasionStatus.NotFound, skipped = skipped, missingLeapThisYear = missingThisYear)
}

private fun yearlyDayMax(month: Int): Int {
    return when (month) {
        2 -> 29
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

private fun solarOrNull(year: Int, month: Int, day: Int): LocalDate? {
    return try {
        LocalDate.of(year, month, day)
    } catch (_: DateTimeException) {
        null
    }
}
