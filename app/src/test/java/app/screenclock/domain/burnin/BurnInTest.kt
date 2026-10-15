package app.screenclock.domain.burnin

import kotlin.math.abs
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BurnInTest {
    private val idle = IDLE_DEFAULT_MILLIS
    private val run = RUN_DEFAULT_MILLIS

    @Test
    fun oneRoundThenStaysSpentUntilTouch() {
        var state = freshBurnInClock(enabled = true, nowMillis = 0L)
        assertEquals(BurnInPhase.Armed, state.phase)

        state = burnInTick(state, idle, run, idle - 1L)
        assertEquals(BurnInPhase.Armed, state.phase)

        state = burnInTick(state, idle, run, idle)
        assertEquals(BurnInPhase.Running, state.phase)
        assertEquals(idle, state.anchorMillis)

        state = burnInTick(state, idle, run, idle + run - 1L)
        assertEquals(BurnInPhase.Running, state.phase)

        state = burnInTick(state, idle, run, idle + run)
        assertEquals(BurnInPhase.Spent, state.phase)

        state = burnInTick(state, idle, run, idle + run + 24L * 60L * 60L * 1000L)
        assertEquals(BurnInPhase.Spent, state.phase)
        assertNull(millisUntilBurnInEdge(state, idle, run, idle + run + 1_000L))
    }

    @Test
    fun touchDuringTheRoundRearmsAndDoesNotStartAgainImmediately() {
        var state = runningAt(start = 0L)
        state = burnInTouch(state, nowMillis = 1_000L)
        assertEquals(BurnInPhase.Armed, state.phase)
        assertEquals(1_000L, state.anchorMillis)

        state = burnInTick(state, idle, run, 1_000L + idle - 1L)
        assertEquals(BurnInPhase.Armed, state.phase)
        state = burnInTick(state, idle, run, 1_000L + idle)
        assertEquals(BurnInPhase.Running, state.phase)
    }

    @Test
    fun touchAfterSpentStartsAFreshWait() {
        var state = freshBurnInClock(enabled = true, nowMillis = 0L)
        state = burnInTick(state, idle, run, idle)
        state = burnInTick(state, idle, run, idle + run)
        assertEquals(BurnInPhase.Spent, state.phase)

        state = burnInTouch(state, nowMillis = 9_000L)
        assertEquals(BurnInPhase.Armed, state.phase)
        state = burnInTick(state, idle, run, 9_000L)
        assertEquals(BurnInPhase.Armed, state.phase)
        state = burnInTick(state, idle, run, 9_000L + idle)
        assertEquals(BurnInPhase.Running, state.phase)
    }

    @Test
    fun touchWhileArmedOnlyResetsTheWait() {
        val state = freshBurnInClock(enabled = true, nowMillis = 0L)
        val touched = burnInTouch(state, nowMillis = 4_000L)
        assertEquals(BurnInPhase.Armed, touched.phase)
        assertEquals(4_000L, touched.anchorMillis)
        assertEquals(BurnInPhase.Armed, burnInTick(touched, idle, run, 4_000L + idle - 1L).phase)
    }

    @Test
    fun offIgnoresTouchAndTime() {
        val off = freshBurnInClock(enabled = false, nowMillis = 0L)
        assertEquals(BurnInPhase.Off, burnInTouch(off, 5_000L).phase)
        assertEquals(BurnInPhase.Off, burnInTick(off, idle, run, idle + run).phase)
        assertNull(millisUntilBurnInEdge(off, idle, run, 0L))
    }

    @Test
    fun disableStopsARunningRound() {
        val running = runningAt(start = 1_000L)
        val off = burnInSetEnabled(running, enabled = false, nowMillis = 2_000L)
        assertEquals(BurnInPhase.Off, off.phase)
        assertEquals(BurnInPhase.Off, burnInTick(off, idle, run, 2_000L + idle + run).phase)
    }

    @Test
    fun enableWhileAlreadyOnDoesNotResetTheTimer() {
        val armed = freshBurnInClock(enabled = true, nowMillis = 0L)
        val again = burnInSetEnabled(armed, enabled = true, nowMillis = 3_000L)
        assertEquals(0L, again.anchorMillis)
        assertEquals(BurnInPhase.Armed, again.phase)
    }

    @Test
    fun enableFromOffArmsAtThatInstant() {
        val off = freshBurnInClock(enabled = false, nowMillis = 0L)
        val armed = burnInSetEnabled(off, enabled = true, nowMillis = 8_000L)
        assertEquals(BurnInPhase.Armed, armed.phase)
        assertEquals(8_000L, armed.anchorMillis)
        assertEquals(BurnInPhase.Armed, burnInTick(armed, idle, run, 8_000L + idle - 1L).phase)
    }

    @Test
    fun pauseBlocksTheScreensaverAndLeavingRearms() {
        var state = freshBurnInClock(enabled = true, nowMillis = 0L)
        state = burnInSetPagePaused(state, paused = true, nowMillis = 500L)
        state = burnInTick(state, idle, run, idle + run)
        assertEquals(BurnInPhase.Armed, state.phase)
        assertTrue(state.paused)
        assertNull(millisUntilBurnInEdge(state, idle, run, idle))

        state = burnInSetPagePaused(state, paused = false, nowMillis = 9_000L)
        assertEquals(BurnInPhase.Armed, state.phase)
        assertEquals(9_000L, state.anchorMillis)
        assertFalse(state.paused)
        assertEquals(BurnInPhase.Armed, burnInTick(state, idle, run, 9_000L + idle - 1L).phase)
        assertEquals(BurnInPhase.Running, burnInTick(state, idle, run, 9_000L + idle).phase)
    }

    @Test
    fun enteringPauseExitsTheScreensaverWithoutSpendingTheRound() {
        val running = runningAt(start = 0L)
        val paused = burnInSetPagePaused(running, paused = true, nowMillis = running.anchorMillis + 1_000L)
        assertEquals(BurnInPhase.Armed, paused.phase)
        assertTrue(paused.paused)
        assertEquals(running.anchorMillis, paused.anchorMillis)

        val left = burnInSetPagePaused(paused, paused = false, nowMillis = running.anchorMillis + 4_000L)
        assertEquals(BurnInPhase.Armed, left.phase)
        assertEquals(running.anchorMillis + 4_000L, left.anchorMillis)
        assertEquals(BurnInPhase.Armed, burnInTick(left, idle, run, left.anchorMillis).phase)
    }

    @Test
    fun pauseOnSpentOrOffDoesNotRearmWhenLeaving() {
        val running = runningAt(0L)
        val spent = burnInTick(running, idle, run, running.anchorMillis + run)
        val afterSpent = burnInSetPagePaused(
            burnInSetPagePaused(spent, paused = true, nowMillis = 10L),
            paused = false,
            nowMillis = 20L,
        )
        assertEquals(BurnInPhase.Spent, afterSpent.phase)
        assertEquals(spent.anchorMillis, afterSpent.anchorMillis)

        val off = freshBurnInClock(enabled = false, nowMillis = 0L)
        val afterOff = burnInSetHostHidden(
            burnInSetHostHidden(off, hidden = true, nowMillis = 10L),
            hidden = false,
            nowMillis = 20L,
        )
        assertEquals(BurnInPhase.Off, afterOff.phase)
    }

    @Test
    fun oneHoldEndingDoesNotRearmWhileTheOtherHoldRemains() {
        var state = freshBurnInClock(enabled = true, nowMillis = 0L)
        state = burnInSetPagePaused(state, paused = true, nowMillis = 1_000L)
        state = burnInSetHostHidden(state, hidden = true, nowMillis = 2_000L)
        state = burnInSetPagePaused(state, paused = false, nowMillis = 3_000L)
        assertTrue(state.paused)
        assertEquals(0L, state.anchorMillis)
        assertEquals(BurnInPhase.Armed, burnInTick(state, idle, run, idle).phase)

        state = burnInSetHostHidden(state, hidden = false, nowMillis = 4_000L)
        assertFalse(state.paused)
        assertEquals(4_000L, state.anchorMillis)
    }

    @Test
    fun enableWhilePausedDoesNotRunUntilTheHoldEnds() {
        var state = freshBurnInClock(enabled = false, nowMillis = 0L, pagePaused = true)
        state = burnInSetEnabled(state, enabled = true, nowMillis = 1_000L)
        state = burnInTick(state, idle, run, 1_000L + idle + run)
        assertEquals(BurnInPhase.Armed, state.phase)
        assertTrue(state.paused)

        state = burnInSetPagePaused(state, paused = false, nowMillis = 5_000L)
        assertEquals(5_000L, state.anchorMillis)
        assertEquals(BurnInPhase.Armed, burnInTick(state, idle, run, 5_000L).phase)
    }

    @Test
    fun clockMovedBackwardsDoesNotFire() {
        val armed = freshBurnInClock(enabled = true, nowMillis = 10_000L)
        val repaired = burnInTick(armed, idle, run, nowMillis = 9_000L)
        assertEquals(BurnInPhase.Armed, repaired.phase)
        assertEquals(9_000L, repaired.anchorMillis)
        assertEquals(0L, millisUntilBurnInEdge(armed, idle, run, nowMillis = 9_000L))
        assertEquals(BurnInPhase.Armed, burnInTick(repaired, idle, run, 9_000L + idle - 1L).phase)
    }

    @Test
    fun shorterIdleIsHonoredFromTheSameAnchor() {
        val armed = freshBurnInClock(enabled = true, nowMillis = 0L)
        val soon = burnInTick(armed, idleMillis = 60_000L, runMillis = run, nowMillis = 60_000L)
        assertEquals(BurnInPhase.Running, soon.phase)
    }

    @Test
    fun durationsClampIntoTheClosedRange() {
        assertEquals(IDLE_MIN_MILLIS, normalizeIdleMillis(0L))
        assertEquals(IDLE_MIN_MILLIS, normalizeIdleMillis(-5L))
        assertEquals(IDLE_DEFAULT_MILLIS, normalizeIdleMillis(IDLE_DEFAULT_MILLIS))
        assertEquals(IDLE_MAX_MILLIS, normalizeIdleMillis(IDLE_MAX_MILLIS))
        assertEquals(IDLE_MAX_MILLIS, normalizeIdleMillis(IDLE_MAX_MILLIS + 60_000L))

        assertEquals(RUN_MIN_MILLIS, normalizeRunMillis(0L))
        assertEquals(RUN_MIN_MILLIS, normalizeRunMillis(9_000L))
        assertEquals(RUN_MIN_MILLIS, normalizeRunMillis(RUN_MIN_MILLIS))
        assertEquals(RUN_DEFAULT_MILLIS, normalizeRunMillis(RUN_DEFAULT_MILLIS))
        assertEquals(RUN_MAX_MILLIS, normalizeRunMillis(RUN_MAX_MILLIS))
        assertEquals(RUN_MAX_MILLIS, normalizeRunMillis(RUN_MAX_MILLIS + 10_000L))
        assertEquals(20_000L, normalizeRunMillis(15_000L))
    }

    @Test
    fun outOfRangeIdleCannotSkipTheMinimumWait() {
        val armed = freshBurnInClock(enabled = true, nowMillis = 0L)
        assertEquals(BurnInPhase.Armed, burnInTick(armed, idleMillis = 0L, runMillis = 0L, nowMillis = 10_000L).phase)
        assertEquals(BurnInPhase.Running, burnInTick(armed, idleMillis = 0L, runMillis = 0L, nowMillis = IDLE_MIN_MILLIS).phase)
    }

    @Test
    fun edgeDelayMatchesTheRemainingWait() {
        val armed = freshBurnInClock(enabled = true, nowMillis = 0L)
        assertEquals(idle, millisUntilBurnInEdge(armed, idle, run, 0L))
        assertEquals(idle - 1_000L, millisUntilBurnInEdge(armed, idle, run, 1_000L))
        assertEquals(0L, millisUntilBurnInEdge(armed, idle, run, idle))

        val running = burnInTick(armed, idle, run, idle)
        assertEquals(run, millisUntilBurnInEdge(running, idle, run, running.anchorMillis))
        assertEquals(0L, millisUntilBurnInEdge(running, idle, run, running.anchorMillis + run))
    }

    @Test
    fun driftStaysOnAOneAndAHalfPixelCircle() {
        assertOffset(1.5f, 0f, driftOffset(0L))
        assertOffset(0f, 1.5f, driftOffset(DRIFT_PERIOD_MILLIS / 4))
        assertOffset(-1.5f, 0f, driftOffset(DRIFT_PERIOD_MILLIS / 2))
        assertOffset(0f, -1.5f, driftOffset(DRIFT_PERIOD_MILLIS * 3 / 4))
        listOf(0L, 1_000L, 15_000L, 59_999L).forEach { time ->
            val offset = driftOffset(time)
            assertEquals(DRIFT_AMPLITUDE_PX, hypot(offset.xPx, offset.yPx), 0.001f)
        }
    }

    @Test
    fun wanderStaysInsideTheTravelBoxAndStartsAtCenter() {
        assertOffset(0f, 0f, wanderOffset(0L, 30f, 40f))
        assertOffset(0f, 0f, wanderOffset(-1L, 30f, 40f))
        assertOffset(0f, 0f, wanderOffset(1_000L, 0f, 0f))
        val quarter = wanderOffset(WANDER_PERIOD_MILLIS / 4, 30f, 40f)
        assertEquals(30f, quarter.xPx, 0.001f)
        assertEquals(0f, quarter.yPx, 0.001f)
        val eighth = wanderOffset(WANDER_PERIOD_MILLIS / 8, 30f, 40f)
        assertEquals(40f, eighth.yPx, 0.001f)
        var time = 0L
        while (time <= WANDER_PERIOD_MILLIS) {
            val offset = wanderOffset(time, 30f, 40f)
            assertTrue(abs(offset.xPx) <= 30.001f)
            assertTrue(abs(offset.yPx) <= 40.001f)
            time += 1_000L
        }
    }

    @Test
    fun motionFollowsPhaseAndPause() {
        assertEquals(BurnInMotion.Still, burnInMotion(BurnInPhase.Off, paused = false))
        assertEquals(BurnInMotion.Drift, burnInMotion(BurnInPhase.Armed, paused = false))
        assertEquals(BurnInMotion.Drift, burnInMotion(BurnInPhase.Spent, paused = false))
        assertEquals(BurnInMotion.Wander, burnInMotion(BurnInPhase.Running, paused = false))
        assertEquals(BurnInMotion.Still, burnInMotion(BurnInPhase.Armed, paused = true))
        assertEquals(BurnInMotion.Still, burnInMotion(BurnInPhase.Running, paused = true))
    }

    private fun runningAt(start: Long): BurnInClock {
        val armed = freshBurnInClock(enabled = true, nowMillis = start)
        return burnInTick(armed, idle, run, start + idle)
    }

    private fun assertOffset(x: Float, y: Float, offset: BurnInOffset) {
        assertEquals(x, offset.xPx, 0.001f)
        assertEquals(y, offset.yPx, 0.001f)
    }
}
