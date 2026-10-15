package app.screenclock.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test

class TubeGlowTest {
    @Test
    fun onlyTheChangedDigitGlowsAtThePeak() {
        val frame = tubeFrame(from = "10", to = "11", progress = 0.5f, animate = true)
        assertEquals('1', frame[0].char)
        assertEquals(0f, frame[0].brightness, 0.001f)
        assertEquals('1', frame[1].char)
        assertEquals(1f, frame[1].brightness, 0.001f)
    }

    @Test
    fun bothDigitsGlowWhenTheWholePairChanges() {
        val frame = tubeFrame(from = "59", to = "00", progress = 0.5f, animate = true)
        assertEquals(1f, frame[0].brightness, 0.001f)
        assertEquals(1f, frame[1].brightness, 0.001f)
        assertEquals("00", frame.joinToString("") { it.char.toString() })
    }

    @Test
    fun pulseRisesAndFallsOnce() {
        assertEquals(0f, tubePulse(0f), 0.001f)
        assertEquals(0.5f, tubePulse(0.25f), 0.001f)
        assertEquals(1f, tubePulse(0.5f), 0.001f)
        assertEquals(0.5f, tubePulse(0.75f), 0.001f)
        assertEquals(0f, tubePulse(1f), 0.001f)
    }

    @Test
    fun finishedOrDisabledGlowRestsOnTheNewDigits() {
        assertRested(tubeFrame(from = "59", to = "00", progress = 1f, animate = true), "00")
        assertRested(tubeFrame(from = "59", to = "00", progress = 0.5f, animate = false), "00")
        assertRested(tubeFrame(from = "30", to = "30", progress = 0.5f, animate = true), "30")
    }

    @Test
    fun tenFiftyNineToElevenGlowsEachChangedDigit() {
        val before = formatFaceTime(10, 59, HourMode.H24, systemIs24Hour = true)
        val after = formatFaceTime(11, 0, HourMode.H24, systemIs24Hour = true)
        val hour = tubeFrame(before.hourDigits, after.hourDigits, 0.5f, animate = true)
        val minute = tubeFrame(before.minuteDigits, after.minuteDigits, 0.5f, animate = true)
        assertEquals(0f, hour[0].brightness, 0.001f)
        assertEquals(1f, hour[1].brightness, 0.001f)
        assertEquals(1f, minute[0].brightness, 0.001f)
        assertEquals(1f, minute[1].brightness, 0.001f)
    }

    private fun assertRested(frame: List<TubeGlyph>, digits: String) {
        assertEquals(digits, frame.joinToString("") { it.char.toString() })
        frame.forEach { glyph -> assertEquals(0f, glyph.brightness, 0.001f) }
    }
}
