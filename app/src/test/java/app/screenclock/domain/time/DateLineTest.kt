package app.screenclock.domain.time

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateLineTest {
    @Test
    fun octoberFirstIsThursdayWithoutAHoliday() {
        assertEquals("10月1日 周四", formatDateLine(LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun octoberFourteenthIsWednesday() {
        assertEquals("10月14日 周三", formatDateLine(LocalDate.of(2026, 10, 14)))
    }
}
