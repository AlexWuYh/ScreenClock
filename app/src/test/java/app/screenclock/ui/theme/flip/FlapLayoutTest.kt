package app.screenclock.ui.theme.flip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlapLayoutTest {
    @Test
    fun portraitKeepsBothFlapsOnOneRow() {
        val layout = flapLayout(width = 360f, height = 640f, stacked = clockStacked(360f, 640f), reserveSide = 0f)
        assertFalse(layout.stacked)
        assertTrue(layout.cardWidth * 2f + layout.gap <= 360f + 0.05f)
        assertTrue(layout.cardWidth <= 360f + 0.01f)
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
    fun aCrampedHeightStillFitsBothFlaps() {
        val layout = flapLayout(width = 400f, height = 100f, stacked = false, reserveSide = 0f)
        assertEquals(100f, layout.cardHeight, 0.05f)
        assertTrue(layout.cardWidth * 2f + layout.gap <= 400f + 0.05f)
        assertEquals(FLAP_GAP_FRACTION, layout.gap / layout.cardWidth, 0.001f)
    }

    @Test
    fun threeFlapsFitPortraitAndLandscape() {
        val portrait = flapLayout(width = 360f, height = 640f, stacked = false, reserveSide = 0f, count = 3)
        val portraitUsed = portrait.cardWidth * 3f + portrait.gap * 2f
        assertTrue(portraitUsed <= 360f + 0.05f)
        assertEquals(FLAP_GAP_FRACTION, portrait.gap / portrait.cardWidth, 0.001f)

        val landscape = flapLayout(width = 800f, height = 360f, stacked = false, reserveSide = 0f, count = 3)
        val landscapeUsed = landscape.cardWidth * 3f + landscape.gap * 2f
        assertTrue(landscapeUsed <= 800f + 0.05f)
        assertEquals(FLAP_ASPECT, landscape.cardWidth / landscape.cardHeight, 0.001f)
    }

    @Test
    fun everyScreenSharesOneRow() {
        assertFalse(clockStacked(width = 360f, height = 640f))
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
        val horizontal = fit.inkLeft
        val vertical = fit.inkTop
        val dx = corner - horizontal
        val dy = corner - vertical
        val insideArc = horizontal >= corner || vertical >= corner || dx * dx + dy * dy <= corner * corner
        assertTrue("ink must clear the rounded corner", insideArc)
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
        val portrait = flapLayout(portraitWidth, portraitHeight, stacked = false, reserveSide = 0f)
        assertFalse(portrait.stacked)
        assertTrue(portrait.cardWidth * 2f + portrait.gap <= portraitWidth + 0.05f)
        assertTrue(portrait.cardHeight <= portraitHeight + 0.05f)
    }
}
