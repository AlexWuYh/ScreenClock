package app.screenclock.domain.time

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateLineTest {
    private val octoberFirst = LocalDate.of(2026, 10, 1)

    @Test
    fun everyPieceOnMatchesTheReferenceLine() {
        assertEquals(
            "2026年10月1日 八月初十 星期四 · 国庆节",
            formatDateLine(
                octoberFirst,
                lunarLabel = "八月初十",
                holidayName = "国庆节",
            ),
        )
    }

    @Test
    fun aHolidayNameIsAppendedWhenOneIsSupplied() {
        assertEquals(
            "2026年10月1日 星期四 · 国庆节",
            formatDateLine(octoberFirst, holidayName = "国庆节", parts = DateParts(lunar = false)),
        )
    }

    @Test
    fun eachPieceCanBeSwitchedOffAlone() {
        assertEquals(
            "2026年10月1日 八月初十",
            formatDateLine(octoberFirst, "八月初十", "国庆节", DateParts(weekday = false, holiday = false)),
        )
        assertEquals(
            "2026年10月1日 星期四",
            formatDateLine(octoberFirst, "八月初十", "国庆节", DateParts(lunar = false, holiday = false)),
        )
        assertEquals(
            "2026年10月1日",
            formatDateLine(
                octoberFirst,
                "八月初十",
                "国庆节",
                DateParts(weekday = false, lunar = false, holiday = false),
            ),
        )
    }

    @Test
    fun aMissingLunarLabelOrHolidayIsSkippedNotSpaced() {
        assertEquals(
            "2026年10月1日 星期四",
            formatDateLine(octoberFirst, lunarLabel = "  ", holidayName = null),
        )
    }

    @Test
    fun wednesdayIsSpelledInChinese() {
        assertEquals(
            "2026年10月14日 星期三",
            formatDateLine(LocalDate.of(2026, 10, 14), parts = DateParts(lunar = false, holiday = false)),
        )
    }
}
