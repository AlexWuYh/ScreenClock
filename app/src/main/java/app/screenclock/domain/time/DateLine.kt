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
