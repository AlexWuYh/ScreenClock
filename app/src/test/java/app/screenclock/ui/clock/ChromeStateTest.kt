package app.screenclock.ui.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromeStateTest {
    @Test
    fun blankTapOnHiddenFaceShowsChromeAndArmsHide() {
        val next = ChromeState().onBlankTap(1_000L)
        assertTrue(next.visible)
        assertEquals(1_000L + ChromeState.HIDE_AFTER_MILLIS, next.hideAtEpochMillis)
    }

    @Test
    fun blankTapOnVisibleChromeHidesIt() {
        val shown = ChromeState().onBlankTap(1_000L)
        val hidden = shown.onBlankTap(2_000L)
        assertFalse(hidden.visible)
        assertNull(hidden.hideAtEpochMillis)
    }

    @Test
    fun visibleChromeWaitsEightSecondsAfterItsLastAction() {
        val shown = ChromeState().onBlankTap(1_000L)
        val refreshed = shown.onInteraction(4_500L)
        assertEquals(12_500L, refreshed.hideAtEpochMillis)
        assertTrue(refreshed.after(5_000L).visible)
        assertFalse(refreshed.after(12_500L).visible)
        assertEquals(ChromeState(), ChromeState().onInteraction(9_000L))
    }

    @Test
    fun timeReachingHideAtHidesChrome() {
        val shown = ChromeState().onBlankTap(1_000L)
        assertEquals(shown, shown.after(8_999L))
        val hidden = shown.after(9_000L)
        assertFalse(hidden.visible)
        assertNull(hidden.hideAtEpochMillis)
    }

    @Test
    fun hiddenChromeIgnoresTime() {
        val hidden = ChromeState()
        assertEquals(hidden, hidden.after(99_000L))
    }

    @Test
    fun customHideDelayIsHonored() {
        val next = ChromeState().onBlankTap(nowEpochMillis = 10L, hideAfterMillis = 250L)
        assertEquals(260L, next.hideAtEpochMillis)
        assertEquals(next, next.after(259L))
        assertFalse(next.after(260L).visible)
    }

    @Test
    fun widthSlotSplitsAtMaterialCompactBreakpoint() {
        assertEquals(ClockWidth.Compact, clockWidth(0))
        assertEquals(ClockWidth.Compact, clockWidth(599))
        assertEquals(ClockWidth.Expanded, clockWidth(600))
        assertEquals(ClockWidth.Expanded, clockWidth(839))
        assertEquals(ClockWidth.Expanded, clockWidth(1200))
    }
}
