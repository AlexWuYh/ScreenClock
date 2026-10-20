package app.screenclock.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class DateLineLayoutTest {
    @Test
    fun longDateDropsOptionalSegmentsBeforeTheCalendarDate() {
        assertEquals(
            listOf(
                "2026年10月16日 九月廿六 星期五 · 国庆节",
                "2026年10月16日 九月廿六 星期五",
                "2026年10月16日 九月廿六",
                "2026年10月16日",
                "10月16日",
            ),
            dateLineCandidates("2026年10月16日 九月廿六 星期五 · 国庆节"),
        )
    }

    @Test
    fun disabledDatePiecesDoNotCreateEmptyOrDuplicateVariants() {
        assertEquals(listOf("10月16日"), dateLineCandidates("10月16日"))
        assertEquals(
            listOf("10月16日 · 国庆节", "10月16日"),
            dateLineCandidates("10月16日 · 国庆节"),
        )
        assertEquals(
            listOf("2026年10月16日 星期五", "2026年10月16日", "10月16日"),
            dateLineCandidates("2026年10月16日 星期五"),
        )
    }

    @Test
    fun compact320dpAtDoubleFontScaleKeepsReadableYearAndDate() {
        val line = "2026年10月16日 九月廿六 星期五 · 国庆节"
        val selection = fitDateLine(line, availableWidthPx = 320 - 32, sizesSp = listOf(16f, 14f)) { text, sp ->
            estimatedWidth(text, sp, fontScale = 2f)
        }
        assertEquals("2026年10月16日" to 16f, selection)
    }

    @Test
    fun veryNarrowScreenKeepsMonthAndDayByReducingFont() {
        val selection = fitDateLine(
            "2026年10月16日 九月廿六 星期五 · 国庆节",
            availableWidthPx = 160,
            sizesSp = listOf(16f, 14f),
        ) { text, sp -> estimatedWidth(text, sp, fontScale = 2.5f) }
        assertEquals("10月16日" to 14f, selection)
    }

    private fun estimatedWidth(text: String, sp: Float, fontScale: Float): Int =
        (text.sumOf { if (it.code < 128) 0.55 else 1.0 } * sp * fontScale).toInt()
}
