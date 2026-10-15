package app.screenclock.domain.occasion

import app.screenclock.data.occasion.SixTailCalendar
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every solar date in these tests comes from cn.6tail:lunar.
 * Method names include that solar date so the anchor is not a hand calculation.
 */
class OccasionLunarTest {
    private val calendar = SixTailCalendar

    @Test
    fun ordinaryApril21On20010513DoesNotHitLeapApril() {
        val year = leapAprilYear()
        val ordinary = requireNotNull(calendar.solarOf(year, 4, 21, leap = false))
        val leap = requireNotNull(calendar.solarOf(year, 4, 21, leap = true))
        assertNotEquals(ordinary, leap)
        val hit = nextOccurrence(ordinary, lunar(4, 21, leap = false), calendar)
        assertEquals("ordinary lunar 4-21 is $ordinary", OccasionStatus.Today, hit.status)
        assertEquals(ordinary, hit.date)
        val missed = nextOccurrence(ordinary, lunar(4, 21, leap = true), calendar)
        assertNotEquals("leap April must not land on ordinary $ordinary", ordinary, missed.date)
    }

    @Test
    fun leapApril21On20010612DoesNotHitOrdinaryApril() {
        val year = leapAprilYear()
        val leap = requireNotNull(calendar.solarOf(year, 4, 21, leap = true))
        val ordinary = requireNotNull(calendar.solarOf(year, 4, 21, leap = false))
        val hit = nextOccurrence(leap, lunar(4, 21, leap = true), calendar)
        assertEquals("leap lunar 4-21 is $leap", OccasionStatus.Today, hit.status)
        assertEquals(leap, hit.date)
        val missed = nextOccurrence(leap, lunar(4, 21, leap = false), calendar)
        assertNotEquals("ordinary April must not land on leap $leap", leap, missed.date)
        assertNotEquals(ordinary, leap)
    }

    @Test
    fun from20390124MissingLeapAprilSkipsTo20580611() {
        val gap = yearWithoutLeapApril()
        val today = requireNotNull(calendar.solarOf(gap.year, 1, 1, leap = false))
        assertNotEquals(4, calendar.leapMonthOf(calendar.lunarYear(today)))
        val expected = requireNotNull(calendar.solarOf(gap.laterYear, 4, 21, leap = true))
        val next = nextOccurrence(today, lunar(4, 21, leap = true), calendar)
        assertEquals("from $today the next leap April 21 is $expected", expected, next.date)
        assertTrue(next.date!!.isAfter(today))
        assertTrue(next.missingLeapThisYear)
    }

    @Test
    fun lunarNewYear20200125SkipsMonth1WithoutDay30Until20220302() {
        val sample = shortMonth()
        val next = nextOccurrence(sample.today, lunar(sample.month, 30, leap = false), calendar)
        assertEquals(
            "lunar $sample lacks day 30 and the next one is ${sample.later}",
            sample.later,
            next.date,
        )
        assertTrue(next.skipped)
        assertNotEquals(sample.shortYear, calendar.lunarYear(next.date!!))
    }

    private fun leapAprilYear(): Int {
        for (year in 2000..2100) {
            if (calendar.leapMonthOf(year) != 4) continue
            if (calendar.solarOf(year, 4, 21, leap = false) == null) continue
            if (calendar.solarOf(year, 4, 21, leap = true) == null) continue
            return year
        }
        error("library has no leap April 21 between 2000 and 2100")
    }

    private fun yearWithoutLeapApril(): LeapGap {
        for (year in 2020..2080) {
            if (calendar.leapMonthOf(year) == 4) continue
            if (calendar.solarOf(year, 1, 1, leap = false) == null) continue
            for (later in year + 1..year + 19) {
                if (calendar.leapMonthOf(later) != 4) continue
                if (calendar.solarOf(later, 4, 21, leap = true) == null) continue
                return LeapGap(year, later)
            }
        }
        error("library has no gap before a leap April")
    }

    private fun shortMonth(): ShortMonth {
        for (year in 2020..2060) {
            val newYear = calendar.solarOf(year, 1, 1, leap = false) ?: continue
            for (month in 1..12) {
                if (calendar.solarOf(year, month, 30, leap = false) != null) continue
                if (calendar.solarOf(year, month, 1, leap = false) == null) continue
                for (laterYear in year + 1..year + 15) {
                    val later = calendar.solarOf(laterYear, month, 30, leap = false) ?: continue
                    if (!later.isAfter(newYear)) continue
                    return ShortMonth(newYear, year, month, later)
                }
            }
        }
        error("library has no short lunar month in the scan")
    }

    private fun lunar(month: Int, day: Int, leap: Boolean) = Occasion(
        id = "lunar",
        title = "农历",
        kind = OccasionKind.LunarYearly,
        year = null,
        month = month,
        day = day,
        leap = leap,
        message = "到了",
        pinned = false,
    )
}

private data class LeapGap(val year: Int, val laterYear: Int)

private data class ShortMonth(
    val today: LocalDate,
    val shortYear: Int,
    val month: Int,
    val later: LocalDate,
)
