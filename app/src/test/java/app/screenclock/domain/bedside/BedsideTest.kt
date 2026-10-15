package app.screenclock.domain.bedside

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BedsideTest {
    @Test
    fun defaultWindowDimsFromTenUntilSeven() {
        assertFalse(night(false, 22, 0))
        assertFalse(night(true, 21, 0))
        assertTrue(night(true, 22, 0))
        assertTrue(night(true, 23, 30))
        assertTrue(night(true, 0, 0))
        assertTrue(night(true, 6, 59))
        assertFalse(night(true, 7, 0))
    }

    @Test
    fun dayWindowDoesNotWrap() {
        assertFalse(night(true, 8, 59, start = 9, end = 17))
        assertTrue(night(true, 9, 0, start = 9, end = 17))
        assertTrue(night(true, 12, 0, start = 9, end = 17))
        assertTrue(night(true, 16, 59, start = 9, end = 17))
        assertFalse(night(true, 17, 0, start = 9, end = 17))
        assertFalse(night(true, 23, 30, start = 9, end = 17))
    }

    @Test
    fun equalHoursAndDisabledNeverDim() {
        assertFalse(night(true, 12, 0, start = 12, end = 12))
        assertFalse(night(true, 22, 0, start = 22, end = 22))
        assertFalse(night(false, 23, 30, start = 22, end = 7))
        assertFalse(night(false, 12, 0, start = 9, end = 17))
    }

    @Test
    fun unknownHoursUseDefaults() {
        assertEquals(22, bedsidePreferences(true, 99, 7, false, false).startHour)
        assertEquals(7, bedsidePreferences(true, 22, -1, false, false).endHour)
        assertTrue(night(true, 23, 30, start = 99, end = 7))
        assertTrue(night(true, 23, 30, start = 22, end = -1))
        assertFalse(night(true, 12, 0, start = 30, end = 40))
    }

    @Test
    fun warmOnlyWhileNightDimIsActive() {
        assertFalse(warmActive(nightActive = false, warmEnabled = true))
        assertFalse(warmActive(nightActive = true, warmEnabled = false))
        assertTrue(warmActive(nightActive = true, warmEnabled = true))
    }

    @Test
    fun screenStaysOnForFocusOrForChargeWhilePlugged() {
        assertTrue(keepScreenOn(focusHolding = true, chargeEnabled = false, plugged = false))
        assertTrue(keepScreenOn(focusHolding = false, chargeEnabled = true, plugged = true))
        assertTrue(keepScreenOn(focusHolding = true, chargeEnabled = true, plugged = true))
        assertFalse(keepScreenOn(focusHolding = false, chargeEnabled = true, plugged = false))
        assertFalse(keepScreenOn(focusHolding = false, chargeEnabled = false, plugged = true))
        assertFalse(keepScreenOn(focusHolding = false, chargeEnabled = false, plugged = false))
    }

    private fun night(
        enabled: Boolean,
        hour: Int,
        minute: Int,
        start: Int = NIGHT_START_DEFAULT,
        end: Int = NIGHT_END_DEFAULT,
    ): Boolean {
        return nightDimActive(enabled, hour * 60 + minute, start, end)
    }
}
