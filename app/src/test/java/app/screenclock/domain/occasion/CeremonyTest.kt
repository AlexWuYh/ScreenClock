package app.screenclock.domain.occasion

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CeremonyTest {
    private val target = LocalDate.of(2026, 12, 25)

    @Test
    fun theCountdownAlwaysSplitsIntoDaysHoursMinutesSeconds() {
        val now = LocalDateTime.of(2026, 10, 23, 12, 45, 0)
        val parts = ceremonyParts(now, target)
        // 63 days to 25 Dec 00:00, minus the 12h45m already gone on the 23rd's clock.
        assertEquals(CeremonyParts(days = 62, hours = 11, minutes = 15, seconds = 0), parts)
    }

    @Test
    fun theLastDayStillShowsFourBlocks() {
        val start = target.atStartOfDay()
        assertEquals(CeremonyParts(0, 23, 59, 59), ceremonyParts(start.minusHours(24).plusSeconds(1), target))
        assertEquals(CeremonyParts(0, 0, 0, 1), ceremonyParts(start.minusNanos(1_000_000), target))
        assertEquals(CeremonyParts(1, 0, 0, 0), ceremonyParts(start.minusHours(24), target))
    }

    @Test
    fun secondsRoundUpSoZeroOnlyShowsAtTheMoment() {
        val start = target.atStartOfDay()
        assertEquals(1, ceremonyParts(start.minusNanos(500_000_000), target).seconds)
        assertEquals(CeremonyParts(0, 0, 0, 0), ceremonyParts(start, target))
        assertEquals(CeremonyParts(0, 0, 0, 0), ceremonyParts(start.plusHours(5), target))
    }

    @Test
    fun theMorningOfTheDateIsDone() {
        val start = target.atStartOfDay()
        assertEquals(CeremonyMode.Counting, ceremonyMode(start.minusNanos(1), target))
        assertEquals(CeremonyMode.Done, ceremonyMode(start, target))
        assertEquals(CeremonyMode.Done, ceremonyMode(start.plusHours(8), target))
    }

    @Test
    fun theDisplayedSecondDecidesTheNextWake() {
        val start = target.atStartOfDay()
        // Half a second into a displayed second: wake when it changes.
        assertEquals(500L, millisUntilCeremonyTick(start.minusSeconds(10).plusNanos(500_000_000), target))
        // Exactly on a second: a full second away.
        assertEquals(1000L, millisUntilCeremonyTick(start.minusSeconds(10), target))
        assertNull(millisUntilCeremonyTick(start, target))
    }

    @Test
    fun hugeDayCountsKeepAllTheirDigits() {
        assertEquals("05", twoDigits(5))
        assertEquals("365", twoDigits(365))
        assertEquals("00", twoDigits(0))
        assertEquals("00", twoDigits(-3))
    }

    @Test
    fun theFaceCarriesTheIconAndSpeaksTheWholeCountdown() {
        val now = LocalDateTime.of(2026, 12, 20, 8, 30, 15)
        val face = ceremonyFace("生日", OccasionIcon.Cake, "快乐", "2026年12月25日", now, target)
        assertEquals(OccasionIcon.Cake, face.icon)
        assertEquals(CeremonyMode.Counting, face.mode)
        assertEquals("生日，还有4天15小时29分45秒", face.spoken)
        val done = ceremonyFace("生日", OccasionIcon.Cake, "快乐", "2026年12月25日", target.atStartOfDay(), target)
        assertEquals("快乐", done.spoken)
        assertEquals(CeremonyParts(0, 0, 0, 0), done.parts)
    }

    @Test
    fun quickDatesCountFromToday() {
        val today = LocalDate.of(2026, 10, 23)
        assertEquals(LocalDate.of(2026, 10, 30), quickDate(today, QuickDate.InWeek))
        assertEquals(LocalDate.of(2026, 11, 22), quickDate(today, QuickDate.InMonth))
        assertEquals(LocalDate.of(2027, 1, 31), quickDate(today, QuickDate.InHundredDays))
        assertEquals(LocalDate.of(2027, 10, 23), quickDate(today, QuickDate.NextYear))
        // A leap day lands on the 28th the year after.
        assertEquals(LocalDate.of(2025, 2, 28), quickDate(LocalDate.of(2024, 2, 29), QuickDate.NextYear))
    }

    @Test
    fun theLookFollowsTheSystemOnlyWhenAsked() {
        assertTrue(ceremonyIsDark(CeremonyTone.FollowSystem, systemDark = true))
        assertFalse(ceremonyIsDark(CeremonyTone.FollowSystem, systemDark = false))
        assertFalse(ceremonyIsDark(CeremonyTone.Light, systemDark = true))
        assertTrue(ceremonyIsDark(CeremonyTone.Dark, systemDark = false))
    }

    @Test
    fun storedLookAndIconFallBackToDefaults() {
        assertEquals(CeremonyStyle.Card, CeremonyStyle.fromStored(null))
        assertEquals(CeremonyStyle.Immersive, CeremonyStyle.fromStored("Immersive"))
        assertEquals(CeremonyTone.FollowSystem, CeremonyTone.fromStored("sepia"))
        assertEquals(OccasionIcon.Default, OccasionIcon.fromStored("Dragon"))
        assertEquals(OccasionIcon.Plane, OccasionIcon.fromStored("Plane"))
    }
}
