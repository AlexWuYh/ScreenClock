package app.screenclock.domain.occasion

import java.time.LocalDate

/**
 * The only lunar operations the rules need.
 * The implementation calls `cn.6tail:lunar`. Months here are 1–12; leap is a boolean.
 */
interface LunarCalendar {
    fun lunarYear(date: LocalDate): Int

    /** Solar date of this lunar month and day, or null when that year does not have it. */
    fun solarOf(lunarYear: Int, month: Int, day: Int, leap: Boolean): LocalDate?

    /** Leap month in this lunar year, 1–12, or 0 when the year has none. */
    fun leapMonthOf(lunarYear: Int): Int

    /** True when some lunar year has this month as a leap month. */
    fun leapMonthEver(month: Int): Boolean
}
