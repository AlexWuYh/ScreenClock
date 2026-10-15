package app.screenclock.domain.time

import java.time.LocalDate

private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** Chinese date above the flaps: 「M月d日 周X」. A statutory name is appended only when supplied. */
fun formatDateLine(date: LocalDate, holidayName: String? = null): String {
    val week = weekdays[date.dayOfWeek.value - 1]
    val base = "${date.monthValue}月${date.dayOfMonth}日 $week"
    val holiday = holidayName?.trim()?.takeIf { it.isNotEmpty() } ?: return base
    return "$base · $holiday"
}
