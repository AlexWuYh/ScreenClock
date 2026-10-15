package app.screenclock.ui.theme.flip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlapLayoutTest {
    @Test
    fun portraitStackKeepsTheGapAndFitsTheHeight() {
        val layout = flapLayout(width = 360f, height = 640f, stacked = true, reserveSide = 0f)
        assertTrue(layout.stacked)
        assertEquals(360f, layout.cardWidth, 0.01f)
        assertEquals(FLAP_GAP_FRACTION, layout.gap / layout.cardWidth, 0.001f)
        assertTrue(layout.cardHeight * 2f + layout.gap <= 640f)
    }

    @Test
    fun landscapeRowReservesTheSideAndKeepsTheGap() {
        val side = 40f
        val layout = flapLayout(width = 800f, height = 360f, stacked = false, reserveSide = side)
        assertFalse(layout.stacked)
        val used = layout.cardWidth * 2f + layout.gap + side
        assertTrue(used <= 800f + 0.05f)
        assertEquals(FLAP_GAP_FRACTION, layout.gap / layout.cardWidth, 0.001f)
        assertEquals(FLAP_ASPECT, layout.cardWidth / layout.cardHeight, 0.001f)
    }

    @Test
    fun crampedPortraitStillFitsTheHeight() {
        val layout = flapLayout(width = 400f, height = 100f, stacked = true, reserveSide = 0f)
        val used = layout.cardHeight * 2f + layout.gap
        assertEquals(100f, used, 0.05f)
        assertEquals(FLAP_GAP_FRACTION, layout.gap / layout.cardWidth, 0.001f)
    }

    @Test
    fun threeFlapsFitPortraitAndLandscape() {
        val portrait = flapLayout(width = 360f, height = 640f, stacked = true, reserveSide = 0f, count = 3)
        val portraitUsed = portrait.cardHeight * 3f + portrait.gap * 2f
        assertTrue(portraitUsed <= 640f + 0.05f)
        assertEquals(FLAP_GAP_FRACTION, portrait.gap / portrait.cardWidth, 0.001f)

        val landscape = flapLayout(width = 800f, height = 360f, stacked = false, reserveSide = 0f, count = 3)
        val landscapeUsed = landscape.cardWidth * 3f + landscape.gap * 2f
        assertTrue(landscapeUsed <= 800f + 0.05f)
        assertEquals(FLAP_ASPECT, landscape.cardWidth / landscape.cardHeight, 0.001f)
    }

    @Test
    fun portraitStacksAndLandscapeSharesOneRow() {
        assertTrue(clockStacked(width = 360f, height = 640f))
        assertFalse(clockStacked(width = 800f, height = 360f))
        assertFalse(clockStacked(width = 600f, height = 600f))
    }

    @Test
    fun cornerIsEightPercentOfTheShortSide() {
        assertEquals(0.08f, FLAP_CORNER_FRACTION, 0f)
    }
}
