package app.screenclock.domain.occasion

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CeremonyTest {
    @Test
    fun aFullDayOrMoreStaysOnTheDayCount() {
        val tomorrow = LocalDate.of(2026, 1, 2)
        assertEquals(CeremonyMode.Days, ceremonyMode(LocalDateTime.of(2026, 1, 1, 0, 0), tomorrow))
        assertEquals(1L, ceremonyDayCount(LocalDate.of(2026, 1, 1), tomorrow))
        val twoDays = LocalDate.of(2026, 1, 3)
        assertEquals(CeremonyMode.Days, ceremonyMode(LocalDateTime.of(2026, 1, 1, 23, 59), twoDays))
        assertEquals(2L, ceremonyDayCount(LocalDate.of(2026, 1, 1), twoDays))
    }

    @Test
    fun underTwentyFourHoursUsesTheClockUntilTheMorning() {
        val tomorrow = LocalDate.of(2026, 1, 2)
        assertEquals(CeremonyMode.Clock, ceremonyMode(LocalDateTime.of(2026, 1, 1, 0, 0, 1), tomorrow))
        val start = tomorrow.atStartOfDay()
        assertEquals(listOf("23", "00", "00"), ceremonyClockGroups(start.minusHours(23), tomorrow))
        assertEquals(listOf("00", "00", "01"), ceremonyClockGroups(start.minusNanos(1_000_000), tomorrow))
        val justInside = start.minusHours(24).plusNanos(1_000_000)
        assertEquals(CeremonyMode.Clock, ceremonyMode(justInside, tomorrow))
        assertEquals(listOf("23", "59", "59"), ceremonyClockGroups(justInside, tomorrow))
        val oneMillisUnder = start.minus(Duration.ofMillis(24L * 3_600_000L - 1L))
        assertEquals(listOf("23", "59", "59"), ceremonyClockGroups(oneMillisUnder, tomorrow))
        assertEquals(
            "考试，剩余23小时59分59秒",
            ceremonySpoken("考试", CeremonyMode.Clock, 0, ceremonyClockGroups(justInside, tomorrow), "到了"),
        )
        assertEquals(CeremonyMode.Done, ceremonyMode(start, tomorrow))
        assertEquals(CeremonyMode.Done, ceremonyMode(start.plusHours(8), tomorrow))
    }

    @Test
    fun dayGroupsSplitFromTheRightWithoutALeadingZero() {
        assertEquals(listOf("5"), dayGroups(5))
        assertEquals(listOf("42"), dayGroups(42))
        assertEquals(listOf("1", "00"), dayGroups(100))
        assertEquals(listOf("3", "65"), dayGroups(365))
        assertEquals(listOf("10", "00"), dayGroups(1000))
    }

    @Test
    fun daysModeDoesNotTickEverySecond() {
        val later = LocalDate.of(2026, 4, 1)
        val wait = millisUntilCeremonyTick(LocalDateTime.of(2026, 1, 1, 12, 0), later)
        assertEquals(43_200_000L, wait)
        val exact = LocalDate.of(2026, 1, 2)
        assertEquals(1L, millisUntilCeremonyTick(LocalDateTime.of(2026, 1, 1, 0, 0), exact))
        assertEquals(1000L, millisUntilCeremonyTick(LocalDateTime.of(2026, 1, 1, 10, 0), exact))
        assertNull(millisUntilCeremonyTick(exact.atStartOfDay(), exact))
    }

    @Test
    fun dayGroupsOfSmallAndHugeCounts() {
        assertEquals(listOf("0"), dayGroups(0))
        assertEquals(listOf("10"), dayGroups(10))
        assertEquals(listOf("10", "00"), dayGroups(1000))
        assertEquals(listOf("1", "00", "00"), dayGroups(10000))
    }

    @Test
    fun theClockFaceNeverShowsTwentyFourOClock() {
        val occurrence = LocalDate.of(2026, 1, 2)
        val exactlyOneDayEarly = LocalDateTime.of(2026, 1, 1, 0, 0)
        assertEquals(listOf("23", "59", "59"), ceremonyClockGroups(exactlyOneDayEarly, occurrence))
    }

    @Test
    fun spokenCopyFollowsTheMode() {
        assertEquals("考试，还有86天", ceremonySpoken("考试", CeremonyMode.Days, 86, emptyList(), "到了"))
        assertEquals(
            "考试，剩余5小时03分01秒",
            ceremonySpoken("考试", CeremonyMode.Clock, 0, listOf("05", "03", "01"), "到了"),
        )
        assertEquals("到了", ceremonySpoken("考试", CeremonyMode.Done, 0, emptyList(), "到了"))
    }
}
