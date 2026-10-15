package app.screenclock.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HourFormatTest {
    @Test
    fun twentyFourHourNoonMidnightMorningAndAfternoon() {
        assertFace(12, 0, HourMode.H24, systemIs24Hour = false, "12", "00", null, "12点00分")
        assertFace(0, 0, HourMode.H24, systemIs24Hour = false, "00", "00", null, "0点00分")
        assertFace(1, 0, HourMode.H24, systemIs24Hour = false, "01", "00", null, "1点00分")
        assertFace(13, 0, HourMode.H24, systemIs24Hour = false, "13", "00", null, "13点00分")
    }

    @Test
    fun twelveHourNoonMidnightMorningAndAfternoon() {
        assertFace(12, 0, HourMode.H12, systemIs24Hour = true, "12", "00", "下午", "下午12点00分")
        assertFace(0, 0, HourMode.H12, systemIs24Hour = true, "12", "00", "上午", "上午12点00分")
        assertFace(1, 0, HourMode.H12, systemIs24Hour = true, "01", "00", "上午", "上午1点00分")
        assertFace(13, 0, HourMode.H12, systemIs24Hour = true, "01", "00", "下午", "下午1点00分")
    }

    @Test
    fun followSystemUsesTheSystemFlag() {
        assertFace(13, 5, HourMode.FollowSystem, systemIs24Hour = true, "13", "05", null, "13点05分")
        assertFace(13, 5, HourMode.FollowSystem, systemIs24Hour = false, "01", "05", "下午", "下午1点05分")
        assertFace(0, 0, HourMode.FollowSystem, systemIs24Hour = true, "00", "00", null, "0点00分")
        assertFace(0, 0, HourMode.FollowSystem, systemIs24Hour = false, "12", "00", "上午", "上午12点00分")
    }

    @Test
    fun nineOhFivePadsDigitsAndLeavesTheHourUnpaddedInSpeech() {
        assertFace(9, 5, HourMode.H24, systemIs24Hour = true, "09", "05", null, "9点05分")
        assertFace(9, 5, HourMode.H12, systemIs24Hour = false, "09", "05", "上午", "上午9点05分")
    }

    @Test
    fun storedHourModeFallsBackToFollowSystem() {
        assertEquals(HourMode.FollowSystem, HourMode.fromStored(null))
        assertEquals(HourMode.FollowSystem, HourMode.fromStored(""))
        assertEquals(HourMode.FollowSystem, HourMode.fromStored("12"))
        assertEquals(HourMode.H12, HourMode.fromStored("H12"))
        assertEquals(HourMode.H24, HourMode.fromStored("H24"))
        assertEquals(HourMode.FollowSystem, HourMode.fromStored(HourMode.FollowSystem.name))
    }

    private fun assertFace(
        hour: Int,
        minute: Int,
        mode: HourMode,
        systemIs24Hour: Boolean,
        hourDigits: String,
        minuteDigits: String,
        meridiem: String?,
        spoken: String,
    ) {
        val face = formatFaceTime(hour, minute, mode, systemIs24Hour)
        assertEquals(hourDigits, face.hourDigits)
        assertEquals(minuteDigits, face.minuteDigits)
        assertEquals(meridiem, face.meridiem)
        assertEquals(spoken, face.spoken)
        assertFalse(face.spoken.contains("秒"))
        assertFalse(face.hourDigits.contains(":"))
        assertFalse(face.minuteDigits.contains(":"))
    }
}
