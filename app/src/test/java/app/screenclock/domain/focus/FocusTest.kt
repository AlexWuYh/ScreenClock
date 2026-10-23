package app.screenclock.domain.focus

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusTest {
    private val min = 60_000L

    @Test
    fun modesHaveTheirLengthsAndCustomIsClamped() {
        assertEquals(25, FocusPrefs(FocusMode.Focus).minutes())
        assertEquals(5, FocusPrefs(FocusMode.ShortBreak).minutes())
        assertEquals(15, FocusPrefs(FocusMode.LongBreak).minutes())
        assertEquals(30, FocusPrefs(FocusMode.Custom, 30).minutes())
        assertEquals(1, FocusPrefs(FocusMode.Custom, 0).minutes())
        assertEquals(180, FocusPrefs(FocusMode.Custom, 500).minutes())
        assertEquals(25 * min, FocusPrefs().totalMillis())
        assertEquals(FocusMode.Focus, FocusMode.fromStored("nope"))
    }

    @Test
    fun startingSetsAnEndMomentFromTheChosenLength() {
        val session = startFocusSession(FocusPrefs(FocusMode.ShortBreak), 1_000L)
        assertEquals(FocusRun.Running(1_000L + 5 * min), session.run)
        assertEquals(5 * min, session.totalMillis)
        assertEquals(5 * min, session.remainingMillis(1_000L))
        assertEquals(4 * min, session.remainingMillis(1_000L + min))
    }

    @Test
    fun pausingKeepsTheRemainderAndResumingSetsAFreshEnd() {
        val started = startFocusSession(FocusPrefs(), 0L)
        val paused = pauseFocus(started, 10 * min)
        assertEquals(FocusRun.Paused(15 * min), paused.run)
        // Time passing while paused changes nothing.
        assertEquals(15 * min, paused.remainingMillis(99 * min))
        val resumed = resumeFocus(paused, 100 * min)
        assertEquals(FocusRun.Running(115 * min), resumed.run)
    }

    @Test
    fun aPauseAfterTheEndIsAFinish() {
        val started = startFocusSession(FocusPrefs(), 0L)
        assertEquals(FocusRun.Done, pauseFocus(started, 26 * min).run)
    }

    @Test
    fun settlingOnlyFinishesARunningSegmentWhoseEndHasPassed() {
        val started = startFocusSession(FocusPrefs(), 0L)
        assertEquals(started, settleFocus(started, 25 * min - 1))
        assertEquals(FocusRun.Done, settleFocus(started, 25 * min).run)
        val paused = pauseFocus(started, 5 * min)
        assertEquals(paused, settleFocus(paused, 999 * min))
    }

    @Test
    fun elapsedAndProgressFollowTheSegment() {
        val started = startFocusSession(FocusPrefs(), 0L)
        assertEquals(0f, focusProgress(null, 0L), 0f)
        assertEquals(0.4f, focusProgress(started, 10 * min), 0.0001f)
        assertEquals(10 * min, started.elapsedMillis(10 * min))
        assertEquals(1f, focusProgress(skipFocus(started), 0L), 0f)
    }

    @Test
    fun onlyFocusTimeIsCreditedAndOnlyTheElapsedPart() {
        val focus = startFocusSession(FocusPrefs(FocusMode.Focus), 0L)
        assertEquals(10 * min, focusCreditMillis(focus, 10 * min))
        assertEquals(25 * min, focusCreditMillis(focus, 99 * min))
        val rest = startFocusSession(FocusPrefs(FocusMode.ShortBreak), 0L)
        assertEquals(0L, focusCreditMillis(rest, 3 * min))
        val custom = startFocusSession(FocusPrefs(FocusMode.Custom, 30), 0L)
        assertEquals(0L, focusCreditMillis(custom, 3 * min))
    }

    @Test
    fun theDayTotalAddsUpAndRestartsOnANewDay() {
        val d1 = LocalDate.of(2026, 10, 23)
        val d2 = LocalDate.of(2026, 10, 24)
        var stats = FocusStats()
        assertEquals(0L, todayFocusMillis(stats, d1))
        stats = addFocusMillis(stats, d1, 25 * min)
        stats = addFocusMillis(stats, d1, 10 * min)
        assertEquals(35 * min, todayFocusMillis(stats, d1))
        assertEquals(0L, todayFocusMillis(stats, d2))
        stats = addFocusMillis(stats, d2, 5 * min)
        assertEquals(5 * min, todayFocusMillis(stats, d2))
        assertEquals(stats, addFocusMillis(stats, d2, 0L))
    }

    @Test
    fun theTotalReadsInHoursAndMinutes() {
        assertEquals("0分", formatFocusTotal(0L))
        assertEquals("0分", formatFocusTotal(59_000L))
        assertEquals("45分", formatFocusTotal(45 * min))
        assertEquals("2小时", formatFocusTotal(120 * min))
        assertEquals("2小时45分", formatFocusTotal(165 * min))
    }

    @Test
    fun theClockTextRoundsSecondsUpAndAddsHoursFromAnHour() {
        assertEquals("25:00", focusClockText(25 * min))
        assertEquals("25:00", focusClockText(25 * min - 1_000L + 1))
        assertEquals("24:59", focusClockText(25 * min - 1_000L))
        assertEquals("00:01", focusClockText(1L))
        assertEquals("00:00", focusClockText(0L))
        assertEquals("59:59", focusClockText(60 * min - 1_000L))
        assertEquals("1:00:00", focusClockText(60 * min))
        assertEquals("3:00:00", focusClockText(180 * min))
    }

    @Test
    fun theWaitFollowsTheDisplayedSecond() {
        val s = startFocusSession(FocusPrefs(), 0L)
        assertEquals(1_000L, millisUntilFocusDigitChange(s, 0L))
        assertEquals(500L, millisUntilFocusDigitChange(s, 500L))
        assertNull(millisUntilFocusDigitChange(s, 25 * min))
        assertNull(millisUntilFocusDigitChange(pauseFocus(s, min), min))
        assertNull(millisUntilFocusDigitChange(null, 0L))
    }

    @Test
    fun theScreenStaysOnWhileCountingOrShowingTheEndButNotWhilePausedOrIdle() {
        val s = startFocusSession(FocusPrefs(), 0L)
        assertTrue(focusKeepsScreen(s))
        assertTrue(focusKeepsScreen(skipFocus(s)))
        assertFalse(focusKeepsScreen(pauseFocus(s, min)))
        assertFalse(focusKeepsScreen(null))
    }

    @Test
    fun spokenCopyFollowsTheState() {
        val prefs = FocusPrefs()
        assertEquals("准备开始，25分钟", focusSpoken(null, prefs, 0L))
        val s = startFocusSession(prefs, 0L)
        assertEquals("剩余24分0秒", focusSpoken(s, prefs, min))
        assertEquals("已暂停，剩余24分0秒", focusSpoken(pauseFocus(s, min), prefs, 99 * min))
        assertEquals("时间到", focusSpoken(skipFocus(s), prefs, 0L))
        val long = startFocusSession(FocusPrefs(FocusMode.Custom, 90), 0L)
        assertEquals("剩余1小时30分0秒", focusSpoken(long, FocusPrefs(FocusMode.Custom, 90), 0L))
    }
}
