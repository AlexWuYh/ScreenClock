package app.screenclock.domain.time

import java.time.LocalDate

private val weekdays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

/** Which optional pieces the date line carries besides the month and day. */
data class DateParts(
    val year: Boolean = true,
    val weekday: Boolean = true,
    val lunar: Boolean = true,
    val holiday: Boolean = true,
)

/**
 * The line above the clock:「2026年10月16日 九月廿六 星期五 · 国庆节」.
 * The lunar label and the statutory name are supplied by the data layer;
 * this only places them. Each piece joins with a space, the holiday trails
 * behind「 · 」. Turning every piece off still leaves「10月16日」.
 */
fun formatDateLine(
    date: LocalDate,
    lunarLabel: String? = null,
    holidayName: String? = null,
    parts: DateParts = DateParts(),
): String {
    val pieces = buildList {
        val monthDay = "${date.monthValue}月${date.dayOfMonth}日"
        add(if (parts.year) "${date.year}年$monthDay" else monthDay)
        if (parts.lunar) {
            lunarLabel?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        }
        if (parts.weekday) {
            add(weekdays[date.dayOfWeek.value - 1])
        }
    }
    val holiday = if (parts.holiday) {
        holidayName?.trim()?.takeIf { it.isNotEmpty() }
    } else {
        null
    }
    val base = pieces.joinToString(" ")
    return if (holiday == null) base else "$base · $holiday"
}

enum class DateSegmentKind { Date, Lunar, Weekday, Holiday }

data class DateSegment(val kind: DateSegmentKind, val text: String)

/**
 * Splits a line made by [formatDateLine] back into typed pieces so the face can show each
 * kind in its own shape. The pieces are told apart by what they contain, not by position:
 * a solar date has digits, a weekday starts with「星期」, the holiday trails「 · 」, and
 * whatever remains is the lunar label.
 */
fun dateSegments(line: String): List<DateSegment> {
    val holidayAt = line.indexOf(" · ")
    val base = if (holidayAt >= 0) line.substring(0, holidayAt) else line
    val holiday = if (holidayAt >= 0) line.substring(holidayAt + 3).trim() else ""
    val out = ArrayList<DateSegment>()
    for (piece in base.split(' ').filter { it.isNotBlank() }) {
        val kind = when {
            piece.startsWith("星期") -> DateSegmentKind.Weekday
            piece.any { it.isDigit() } -> DateSegmentKind.Date
            else -> DateSegmentKind.Lunar
        }
        out += DateSegment(kind, piece)
    }
    if (holiday.isNotEmpty()) out += DateSegment(DateSegmentKind.Holiday, holiday)
    return out
}
