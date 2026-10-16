package app.screenclock.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LcdDigitsTest {
    @Test
    fun classicDigitsLightTheExpectedSegments() {
        assertEquals(LcdSegment.entries.toSet(), lcdSegments('8'))
        assertEquals(LcdSegment.entries.toSet() - LcdSegment.Middle, lcdSegments('0'))
        assertEquals(setOf(LcdSegment.TopRight, LcdSegment.BottomRight), lcdSegments('1'))
        assertEquals(setOf(LcdSegment.Top, LcdSegment.TopRight, LcdSegment.BottomRight), lcdSegments('7'))
    }

    @Test
    fun fourSixNineKeepTheirDiagonalDistinct() {
        assertEquals(
            setOf(
                LcdSegment.TopLeft,
                LcdSegment.Middle,
                LcdSegment.TopRight,
                LcdSegment.BottomRight,
            ),
            lcdSegments('4'),
        )
        assertTrue(LcdSegment.BottomLeft in lcdSegments('6'))
        assertTrue(LcdSegment.BottomLeft !in lcdSegments('9'))
    }

    @Test
    fun aDashIsOnlyTheMiddleBarAndUnknownGlyphsLightNothing() {
        assertEquals(setOf(LcdSegment.Middle), lcdSegments('-'))
        assertEquals(emptySet<LcdSegment>(), lcdSegments('a'))
        assertEquals(emptySet<LcdSegment>(), lcdSegments(':'))
    }
}
