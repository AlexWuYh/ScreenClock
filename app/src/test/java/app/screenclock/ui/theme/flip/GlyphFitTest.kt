package app.screenclock.ui.theme.flip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphFitTest {
    @Test
    fun tallInkIsLimitedByHeightAndStaysCentered() {
        val fit = glyphFit(cardWidth = 370f, cardHeight = 200f, inkWidth = 80f, inkHeight = 100f, measuredSize = 100f)
        val drawnWidth = 80f * fit.sizeScale
        val drawnHeight = 100f * fit.sizeScale
        assertEquals(200f * GLYPH_HEIGHT_FRACTION, drawnHeight, 0.01f)
        assertTrue(drawnWidth <= 370f * GLYPH_WIDTH_FRACTION + 0.01f)
        assertEquals((370f - drawnWidth) / 2f, fit.inkLeft, 0.01f)
        assertEquals((200f - drawnHeight) / 2f, fit.inkTop, 0.01f)
    }

    @Test
    fun wideInkIsLimitedByWidthAndStaysCentered() {
        val fit = glyphFit(cardWidth = 370f, cardHeight = 200f, inkWidth = 300f, inkHeight = 40f, measuredSize = 50f)
        val drawnWidth = 300f * fit.sizeScale
        val drawnHeight = 40f * fit.sizeScale
        assertEquals(370f * GLYPH_WIDTH_FRACTION, drawnWidth, 0.01f)
        assertTrue(drawnHeight <= 200f * GLYPH_HEIGHT_FRACTION + 0.01f)
        assertEquals((370f - drawnWidth) / 2f, fit.inkLeft, 0.01f)
        assertEquals((200f - drawnHeight) / 2f, fit.inkTop, 0.01f)
        assertEquals(50f * fit.sizeScale, 50f * (370f * GLYPH_WIDTH_FRACTION / 300f), 0.01f)
    }

    @Test
    fun scaledInkClearsTheRoundedCorner() {
        val cardWidth = 185f
        val cardHeight = 100f
        val fit = glyphFit(cardWidth, cardHeight, inkWidth = 100f, inkHeight = 70f, measuredSize = 100f)
        val top = fit.inkTop
        val left = fit.inkLeft
        val corner = minOf(cardWidth, cardHeight) * FLAP_CORNER_FRACTION
        val dx = corner - left
        val dy = corner - top
        val insideArc = left >= corner || top >= corner || dx * dx + dy * dy <= corner * corner
        assertTrue("ink must clear the rounded corner", insideArc)
    }
}
