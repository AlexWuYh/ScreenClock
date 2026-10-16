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
    fun glyphInkStaysInsideTheCardAndClearOfTheCorner() {
        val cardWidth = 360f
        val cardHeight = cardWidth / FLAP_ASPECT
        val fit = glyphFit(
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            inkWidth = cardWidth,
            inkHeight = cardHeight,
            measuredSize = 100f,
        )
        val inkWidth = cardWidth * fit.sizeScale
        val inkHeight = cardHeight * fit.sizeScale
        assertTrue(fit.inkLeft >= 0f)
        assertTrue(fit.inkTop >= 0f)
        assertTrue(fit.inkLeft + inkWidth <= cardWidth)
        assertTrue(fit.inkTop + inkHeight <= cardHeight)
        val corner = minOf(cardWidth, cardHeight) * FLAP_CORNER_FRACTION
        assertTrue("ink must start below the corner radius", fit.inkTop >= corner)
    }

    @Test
    fun honorTabletKeepsTheFlapsInsideTheSafeArea() {
        val density = 249f / 160f
        val landscapeWidth = 2560f / density - 64f
        val landscapeHeight = 1600f / density - 64f - 56f
        val landscape = flapLayout(landscapeWidth, landscapeHeight, stacked = false, reserveSide = 0f)
        assertFalse(landscape.stacked)
        assertTrue(landscape.cardWidth * 2f + landscape.gap <= landscapeWidth + 0.05f)
        assertTrue(landscape.cardHeight <= landscapeHeight + 0.05f)

        val portraitWidth = 1600f / density - 64f
        val portraitHeight = 2560f / density - 64f - 56f
        val portrait = flapLayout(portraitWidth, portraitHeight, stacked = true, reserveSide = 0f)
        assertTrue(portrait.stacked)
        assertTrue(portrait.cardHeight * 2f + portrait.gap <= portraitHeight + 0.05f)
        assertTrue(portrait.cardWidth <= portraitWidth + 0.05f)
    }
}
