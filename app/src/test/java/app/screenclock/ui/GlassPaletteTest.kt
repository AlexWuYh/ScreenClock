package app.screenclock.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassPaletteTest {
    @Test
    fun coolLightManagementPaletteKeepsTranslucentGlass() {
        assertTrue(Paper.blue > Paper.red)
        assertTrue(UiAccentDeep.blue > UiAccentDeep.red)
        assertTrue(GlassFill.alpha in 0.5f..0.85f)
        assertTrue(GlassFillHigh.alpha > GlassFill.alpha)
        assertTrue(GlassBorder.alpha > GlassFillHigh.alpha)
    }

    @Test
    fun clockFaceAmberRemainsUnchangedByManagementRecolor() {
        assertEquals(0xFFE6A15C, Amber.value.toLong() ushr 32)
        assertEquals(0xFF000000, Ink.value.toLong() ushr 32)
    }
}
