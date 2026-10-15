package app.screenclock.domain.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusTest {
    @Test
    fun presetsAndCustomMinutesClampToTheClosedRange() {
        assertEquals(25, FOCUS_PRESET_SHORT_MINUTES)
        assertEquals(50, FOCUS_PRESET_LONG_MINUTES)
        assertEquals(600L, FOCUS_HOLD_MILLIS)
        assertEquals(1, normalizeFocusMinutes(0))
        assertEquals(1, normalizeFocusMinutes(-4))
        assertEquals(25, normalizeFocusMinutes(25))
        assertEquals(180, normalizeFocusMinutes(181))
        assertEquals(180, FOCUS_MAX_MINUTES)
    }

    @Test
    fun endMomentIsNowPlusTheClampedDuration() {
        assertEquals(25L * 60_000L, focusEndEpochMillis(0L, 25))
        assertEquals(1_000L + 60_000L, focusEndEpochMillis(1_000L, 0))
        assertEquals(1_000L + 180L * 60_000L, focusEndEpochMillis(1_000L, 500))
    }

    @Test
    fun missingOrNonPositiveStoredEndIsNotASession() {
        assertNull(storedFocusEnd(null))
        assertNull(storedFocusEnd(0L))
        assertNull(storedFocusEnd(-1L))
        assertEquals(1_700_000_000_000L, storedFocusEnd(1_700_000_000_000L))
    }

    @Test
    fun remainingComesFromTheEndMomentAndRoundsTheSecondUp() {
        val end = focusEndEpochMillis(0L, 25)
        val started = focusFace(end, 0L)
        assertEquals(FocusPhase.Running, started.phase)
        assertEquals(listOf("25", "00"), started.groups)

        assertEquals(listOf("25", "00"), focusFace(end, 1L).groups)
        assertEquals(listOf("24", "59"), focusFace(end, 1_000L).groups)
        assertEquals(listOf("20", "00"), focusFace(end, 5L * 60_000L).groups)
        assertEquals(listOf("00", "02"), focusFace(end, end - 1_001L).groups)
        assertEquals(listOf("00", "01"), focusFace(end, end - 1L).groups)
    }

    @Test
    fun aFullHourUsesThreeGroupsAndUnderAnHourUsesTwo() {
        val end = focusEndEpochMillis(0L, 90)
        assertEquals(listOf("01", "30", "00"), focusFace(end, 0L).groups)
        assertEquals(listOf("01", "00", "00"), focusFace(end, 30L * 60_000L).groups)
        assertEquals(listOf("01", "00", "00"), focusFace(end, 30L * 60_000L + 999L).groups)
        assertEquals(listOf("59", "59"), focusFace(end, 30L * 60_000L + 1_000L).groups)
        assertEquals(listOf("03", "00", "00"), focusFace(focusEndEpochMillis(0L, 180), 0L).groups)
        assertEquals(listOf("01", "00"), focusFace(focusEndEpochMillis(0L, 1), 0L).groups)
    }

    @Test
    fun zeroStaysDoneUntilTheCallerClearsTheEnd() {
        val end = 5_000L
        val done = focusFace(end, end)
        assertEquals(FocusPhase.Done, done.phase)
        assertEquals(listOf("00", "00"), done.groups)
        val later = focusFace(end, end + 60_000L)
        assertEquals(FocusPhase.Done, later.phase)
        assertEquals(listOf("00", "00"), later.groups)
        assertEquals(done, focusFace(end, end))
    }

    @Test
    fun digitChangeWaitsForTheNextRoundedSecond() {
        val end = 10_000L
        assertEquals(1000L, millisUntilFocusDigitChange(end, 0L))
        assertEquals(400L, millisUntilFocusDigitChange(end, 600L))
        assertNull(millisUntilFocusDigitChange(end, end))
        assertNull(millisUntilFocusDigitChange(end, end + 1L))
    }

    @Test
    fun spokenLineMatchesTheGroups() {
        assertEquals("剩余25分00秒", focusSpoken(focusFace(focusEndEpochMillis(0L, 25), 0L)))
        assertEquals("剩余1小时30分00秒", focusSpoken(focusFace(focusEndEpochMillis(0L, 90), 0L)))
        assertEquals("专注结束", focusSpoken(focusFace(1_000L, 1_000L)))
    }
}
