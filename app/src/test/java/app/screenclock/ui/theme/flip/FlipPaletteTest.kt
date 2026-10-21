package app.screenclock.ui.theme.flip

import app.screenclock.ui.FlipBackdrop
import app.screenclock.ui.FlipDarkFlap
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipPaletteTest {
    @Test
    fun defaultCardIsOnlySlightlyDarkerThanItsWarmBlackBackdrop() {
        val backdrop = FlipBackdrop
        val flap = FlipDarkFlap
        assertTrue(flap.red < backdrop.red)
        assertTrue(flap.green < backdrop.green)
        assertTrue(flap.blue < backdrop.blue)
        assertTrue(backdrop.red - flap.red < 0.06f)
        assertTrue(backdrop.green - flap.green < 0.06f)
        assertTrue(backdrop.blue - flap.blue < 0.06f)
    }
}
