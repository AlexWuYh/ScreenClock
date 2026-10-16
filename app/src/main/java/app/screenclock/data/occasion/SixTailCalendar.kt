package app.screenclock.data.occasion

import app.screenclock.domain.occasion.LunarCalendar
import com.nlf.calendar.Lunar
import com.nlf.calendar.LunarYear
import com.nlf.calendar.Solar
import com.nlf.calendar.util.HolidayUtil
import java.time.LocalDate

/**
 * `cn.6tail:lunar` uses a negative month for a leap month. That sign stays inside this file.
 * Stored occasions keep a positive month and a leap flag.
 */
object SixTailCalendar : LunarCalendar {
    private val everLeap: Set<Int> by lazy { scanLeapMonths() }

    override fun lunarYear(date: LocalDate): Int {
        return Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar.year
    }

    /** 「五月十六」for the date line,「闰四月廿一」in a leap month. Library naming, not ours. */
    fun lunarDateLabel(date: LocalDate): String? {
        return try {
            val lunar = Solar.fromYmd(date.year, date.monthValue, date.dayOfMonth).lunar
            val month = lunar.monthInChinese?.takeIf { it.isNotBlank() } ?: return null
            val day = lunar.dayInChinese?.takeIf { it.isNotBlank() } ?: return null
            "$month$day"
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun solarOf(lunarYear: Int, month: Int, day: Int, leap: Boolean): LocalDate? {
        if (month !in 1..12 || day !in 1..30) return null
        val lunarMonth = if (leap) -month else month
        return try {
            val lunar = Lunar.fromYmd(lunarYear, lunarMonth, day)
            // The library rolls some impossible dates forward (a short month's day 30,
            // a leap month that year does not have). Reading the result back keeps
            // only the dates that really are this month, day and leap flag.
            if (lunar.year != lunarYear || lunar.month != lunarMonth || lunar.day != day) return null
            val solar = lunar.solar
            LocalDate.of(solar.year, solar.month, solar.day)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun leapMonthOf(lunarYear: Int): Int {
        return try {
            LunarYear.fromYear(lunarYear).leapMonth
        } catch (_: RuntimeException) {
            0
        }
    }

    override fun leapMonthEver(month: Int): Boolean {
        return month in everLeap
    }
}

/**
 * A statutory rest day contributes one library name. A makeup work day (`isWork`) does not.
 * There is no handwritten festival table.
 */
fun statutoryHolidayName(date: LocalDate): String? {
    val holiday = try {
        HolidayUtil.getHoliday(date.year, date.monthValue, date.dayOfMonth)
    } catch (_: RuntimeException) {
        null
    } ?: return null
    if (holiday.isWork) return null
    val name = holiday.name?.trim().orEmpty()
    return name.ifEmpty { null }
}

private fun scanLeapMonths(): Set<Int> {
    val found = mutableSetOf<Int>()
    for (year in LEAP_SCAN_START..LEAP_SCAN_END) {
        if (found.size == 12) break
        val leap = try {
            LunarYear.fromYear(year).leapMonth
        } catch (_: RuntimeException) {
            0
        }
        if (leap in 1..12) found += leap
    }
    return found
}

private const val LEAP_SCAN_START: Int = 1600
private const val LEAP_SCAN_END: Int = 2500
