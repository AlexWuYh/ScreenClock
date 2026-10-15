package app.screenclock.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FlipFrameTest {
    @Test
    fun progressZeroShowsTheOldTopFoldingDown() {
        val frame = flipFrame(from = "10", to = "11", progress = 0f, animate = true)
        assertEquals("11", frame.topBack)
        assertEquals("10", frame.topFlap)
        assertEquals(0f, frame.topFlapTurns, 0.001f)
        assertEquals("10", frame.bottomBack)
        assertNull(frame.bottomFlap)
    }

    @Test
    fun justBeforeHalfwayTheTopIsAlmostFoldedAndTheBottomIsStillOld() {
        val frame = flipFrame(from = "10", to = "11", progress = 0.49f, animate = true)
        assertEquals("10", frame.topFlap)
        assertEquals(0.98f, frame.topFlapTurns, 0.001f)
        assertEquals("10", frame.bottomBack)
        assertNull(frame.bottomFlap)
    }

    @Test
    fun halfwayStartsTheNewBottomFoldingUp() {
        val frame = flipFrame(from = "59", to = "00", progress = 0.5f, animate = true)
        assertNull(frame.topFlap)
        assertEquals("00", frame.topBack)
        assertEquals("59", frame.bottomBack)
        assertEquals("00", frame.bottomFlap)
        assertEquals(0f, frame.bottomFlapTurns, 0.001f)
    }

    @Test
    fun finishedProgressSettlesOnTheNewDigits() {
        val frame = flipFrame(from = "10", to = "11", progress = 1f, animate = true)
        assertSettled(frame, "11")
    }

    @Test
    fun animatorOffOrUnchangedDigitsSnap() {
        assertSettled(flipFrame(from = "10", to = "11", progress = 0f, animate = false), "11")
        assertSettled(flipFrame(from = "11", to = "11", progress = 0.25f, animate = true), "11")
    }

    @Test
    fun tenFiftyNineToElevenFlipsBothCards() {
        val before = formatFaceTime(10, 59, HourMode.H24, systemIs24Hour = true)
        val after = formatFaceTime(11, 0, HourMode.H24, systemIs24Hour = true)
        val hour = flipFrame(before.hourDigits, after.hourDigits, 0.25f, animate = true)
        val minute = flipFrame(before.minuteDigits, after.minuteDigits, 0.25f, animate = true)
        assertEquals("10", hour.topFlap)
        assertEquals("11", hour.topBack)
        assertEquals("59", minute.topFlap)
        assertEquals("00", minute.topBack)
    }

    private fun assertSettled(frame: FlipFrame, digits: String) {
        assertEquals(digits, frame.topBack)
        assertEquals(digits, frame.bottomBack)
        assertNull(frame.topFlap)
        assertNull(frame.bottomFlap)
    }
}
