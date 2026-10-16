package app.screenclock.domain.occasion

import app.screenclock.data.occasion.SixTailCalendar
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The solar anchors are library generated and pinned as literals, exactly as the
 * method names say, so a library change cannot leave a lying name behind.
 */
class OccasionLunarTest {
    private val calendar = SixTailCalendar

    @Test
    fun ordinaryApril21On20010513DoesNotHitLeapApril() {
        val ordinary = LocalDate.of(2001, 5, 13)
        val leap = LocalDate.of(2001, 6, 12)
        assertEquals(ordinary, calendar.solarOf(2001, 4, 21, leap = false))
        assertEquals(leap, calendar.solarOf(2001, 4, 21, leap = true))
        val hit = nextOccurrence(ordinary, lunar(4, 21, leap = false), calendar)
        assertEquals(OccasionStatus.Today, hit.status)
        assertEquals(ordinary, hit.date)
        val missed = nextOccurrence(ordinary, lunar(4, 21, leap = true), calendar)
        assertNotEquals("leap April must not land on ordinary $ordinary", ordinary, missed.date)
    }

    @Test
    fun leapApril21On20010612DoesNotHitOrdinaryApril() {
        val ordinary = LocalDate.of(2001, 5, 13)
        val leap = LocalDate.of(2001, 6, 12)
        assertEquals(leap, calendar.solarOf(2001, 4, 21, leap = true))
        assertEquals(ordinary, calendar.solarOf(2001, 4, 21, leap = false))
        val hit = nextOccurrence(leap, lunar(4, 21, leap = true), calendar)
        assertEquals(OccasionStatus.Today, hit.status)
        assertEquals(leap, hit.date)
        val missed = nextOccurrence(leap, lunar(4, 21, leap = false), calendar)
        assertNotEquals("ordinary April must not land on leap $leap", leap, missed.date)
    }

    @Test
    fun from20390124MissingLeapAprilSkipsTo20580611() {
        val today = LocalDate.of(2039, 1, 24)
        val expected = LocalDate.of(2058, 6, 11)
        assertEquals(expected, calendar.solarOf(2058, 4, 21, leap = true))
        assertNotEquals(4, calendar.leapMonthOf(calendar.lunarYear(today)))
        val next = nextOccurrence(today, lunar(4, 21, leap = true), calendar)
        assertEquals("from $today the next leap April 21 is $expected", expected, next.date)
        assertTrue(next.date!!.isAfter(today))
        assertTrue(next.missingLeapThisYear)
    }

    @Test
    fun lunarNewYear20200125SkipsMonth1WithoutDay30Until20220302() {
        val today = LocalDate.of(2020, 1, 25)
        val later = LocalDate.of(2022, 3, 2)
        assertEquals(today, calendar.solarOf(2020, 1, 1, leap = false))
        assertNull(calendar.solarOf(2020, 1, 30, leap = false))
        assertNull(calendar.solarOf(2021, 1, 30, leap = false))
        assertEquals(later, calendar.solarOf(2022, 1, 30, leap = false))
        val next = nextOccurrence(today, lunar(1, 30, leap = false), calendar)
        assertEquals("lunar month 1 of 2020 lacks day 30 and the next one is $later", later, next.date)
        assertTrue(next.skipped)
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
