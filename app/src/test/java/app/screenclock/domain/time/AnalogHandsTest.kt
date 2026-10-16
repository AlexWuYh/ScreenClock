package app.screenclock.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalogHandsTest {
    @Test
    fun theMinuteHandRunsSixDegreesPerMinute() {
        assertEquals(0f, handAngles(0, 0).minuteDegrees, 0.001f)
        assertEquals(180f, handAngles(0, 30).minuteDegrees, 0.001f)
        assertEquals(354f, handAngles(0, 59).minuteDegrees, 0.001f)
    }

    @Test
    fun theHourHandCreepsBetweenHourMarks() {
        assertEquals(0f, handAngles(12, 0).hourDegrees, 0.001f)
        assertEquals(90f, handAngles(3, 0).hourDegrees, 0.001f)
        assertEquals(195f, handAngles(6, 30).hourDegrees, 0.001f)
        assertEquals(270f, handAngles(9, 0).hourDegrees, 0.001f)
    }

    @Test
    fun outOfRangeInputStillPointsSomewhereSensible() {
        assertEquals(handAngles(3, 0), handAngles(15, 0))
        assertEquals(handAngles(0, 30).minuteDegrees, handAngles(0, -30).minuteDegrees, 0.001f)
    }
}
