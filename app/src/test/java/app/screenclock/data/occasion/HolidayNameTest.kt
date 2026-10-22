package app.screenclock.data.occasion

import app.screenclock.domain.time.DateParts
import app.screenclock.domain.time.formatDateLine
import com.nlf.calendar.util.HolidayUtil
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidayNameTest {
    @Test
    fun nationalDay2026UsesTheLibraryName() {
        val day = LocalDate.of(2026, 10, 1)
        val name = statutoryHolidayName(day)
        assertEquals("国庆节", name)
        assertEquals("2026年10月1日 星期四 · 国庆节", formatDateLine(day, holidayName = name, parts = DateParts(lunar = false)))
    }

    @Test
    fun aNormalDayHasNoHolidayName() {
        assertNull(statutoryHolidayName(LocalDate.of(2026, 10, 14)))
    }

    @Test
    fun aMakeupWorkDayIsNotShown() {
        var found = false
        var day = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 12, 31)
        while (!day.isAfter(end)) {
            val holiday = HolidayUtil.getHoliday(day.year, day.monthValue, day.dayOfMonth)
            if (holiday != null && holiday.isWork) {
                found = true
                assertNull("makeup ${day} ${holiday.name}", statutoryHolidayName(day))
                break
            }
            day = day.plusDays(1)
        }
        assertTrue(found)
    }

    /**
     * lunar 1.7.7 bundles statutory holiday data only up to the 2026 schedule. Past that the
     * festival segment must disappear quietly, not crash. When a newer lunar release adds
     * 2027, bump the version in libs.versions.toml and replace this with a real assertion.
     */
    @Test
    fun yearsBeyondTheBundledScheduleHaveNoHolidayName() {
        assertNull(statutoryHolidayName(LocalDate.of(2027, 1, 1)))
        assertNull(statutoryHolidayName(LocalDate.of(2027, 10, 1)))
    }
}
