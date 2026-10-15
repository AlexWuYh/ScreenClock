package app.screenclock.domain.time

import java.time.LocalDate

private val weekdays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** Chinese date above the flaps: 「M月d日 周X」. No holiday suffix until M6. */
fun formatDateLine(date: LocalDate): String {
    val week = weekdays[date.dayOfWeek.value - 1]
    return "${date.monthValue}月${date.dayOfMonth}日 $week"
}
