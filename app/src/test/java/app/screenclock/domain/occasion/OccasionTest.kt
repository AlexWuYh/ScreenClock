package app.screenclock.domain.occasion

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OccasionTest {
    private val unused = object : LunarCalendar {
        override fun lunarYear(date: LocalDate): Int = error("solar rules do not ask the lunar calendar")
        override fun solarOf(lunarYear: Int, month: Int, day: Int, leap: Boolean): LocalDate? = error("unused")
        override fun leapMonthOf(lunarYear: Int): Int = error("unused")
        override fun leapMonthEver(month: Int): Boolean = error("unused")
    }

    @Test
    fun yearlyFebruary29SkipsToTheNextRealLeapDay() {
        val next = nextOccurrence(LocalDate.of(2026, 1, 1), yearly(2, 29), unused)
        assertEquals(OccasionStatus.Future, next.status)
        assertEquals(LocalDate.of(2028, 2, 29), next.date)
        assertTrue(next.skipped)
        assertEquals("有的年份没有这一天，下次是 2028年2月29日", previewText(next))
    }

    @Test
    fun yearlyFebruary29OnThatDayIsToday() {
        val next = nextOccurrence(LocalDate.of(2028, 2, 29), yearly(2, 29), unused)
        assertEquals(OccasionStatus.Today, next.status)
        assertEquals(LocalDate.of(2028, 2, 29), next.date)
        assertEquals("就是今天", previewText(next))
    }

    @Test
    fun solarOnceBeforeTodayIsPassedAndStaysInTheList() {
        val item = once("exam", 2026, 1, 1, pinned = true)
        val today = LocalDate.of(2026, 3, 1)
        val next = nextOccurrence(today, item, unused)
        assertEquals(OccasionStatus.Passed, next.status)
        assertEquals(LocalDate.of(2026, 1, 1), next.date)
        assertEquals("已过", previewText(next))
        assertTrue(canKeep(next.status))
        assertFalse(canPin(next.status))
        val refreshed = refreshPins(listOf(item), today, unused)
        assertEquals(item.id, refreshed.single().id)
        assertFalse(refreshed.single().pinned)
        assertNull(occasionLine(refreshed.single(), today.atStartOfDay(), unused))
    }

    @Test
    fun normalYearlyPreviewNamesTheNextSolarDate() {
        val next = nextOccurrence(LocalDate.of(2026, 6, 1), yearly(5, 28), unused)
        assertEquals(LocalDate.of(2027, 5, 28), next.date)
        assertFalse(next.skipped)
        assertEquals("下次是 2027年5月28日", previewText(next))
    }

    @Test
    fun impossibleDayCannotBeSaved() {
        val next = nextOccurrence(LocalDate.of(2026, 1, 1), yearly(2, 31), unused)
        assertEquals(OccasionStatus.NotFound, next.status)
        assertEquals("近期没有这一天", previewText(next))
        assertFalse(canKeep(next.status))
        val once = nextOccurrence(LocalDate.of(2026, 1, 1), once("x", 2026, 2, 31), unused)
        assertEquals(OccasionStatus.NotFound, once.status)
    }

    @Test
    fun missingLeapMonthNamesTheLaterSolarDate() {
        val calendar = script(
            yearOf = { 2026 },
            leapOf = { year -> if (year == 2027) 4 else 0 },
            ever = { month -> month == 4 },
            solar = { year, month, day, leap ->
                if (year == 2027 && month == 4 && day == 21 && leap) LocalDate.of(2027, 5, 28) else null
            },
        )
        val next = nextOccurrence(LocalDate.of(2026, 3, 1), lunar(4, 21, leap = true), calendar)
        assertEquals("今年没有这个闰月，下次是 2027年5月28日", previewText(next))
    }

    @Test
    fun aShortMonthSaysTheDayWillBeDeferred() {
        val calendar = script(
            yearOf = { 2026 },
            ever = { true },
            solar = { year, month, day, leap ->
                if (!leap && year == 2027 && month == 4 && day == 30) LocalDate.of(2027, 6, 3) else null
            },
        )
        val next = nextOccurrence(LocalDate.of(2026, 1, 1), lunar(4, 30, leap = false), calendar)
        assertTrue(next.skipped)
        assertEquals("有的年份没有这一天，下次是 2027年6月3日", previewText(next))
    }

    @Test
    fun aLeapMonthTheLibraryNeverHasCannotBeSaved() {
        val calendar = script(ever = { false })
        val next = nextOccurrence(LocalDate.of(2026, 1, 1), lunar(2, 1, leap = true), calendar)
        assertEquals(OccasionStatus.NotFound, next.status)
        assertEquals("近期没有这一天", previewText(next))
        assertFalse(canKeep(next.status))
    }

    @Test
    fun pinningASecondItemUnpinsTheFirst() {
        val today = LocalDate.of(2026, 10, 15)
        val first = once("a", 2026, 12, 1, pinned = true)
        val second = once("b", 2026, 12, 2)
        val pinned = pinOccasion(listOf(first, second), "b", today, unused)
        assertFalse(pinned[0].pinned)
        assertTrue(pinned[1].pinned)
    }

    @Test
    fun aPassedOnceCannotBePinned() {
        val today = LocalDate.of(2026, 3, 1)
        val past = once("a", 2026, 1, 1)
        val pinned = pinOccasion(listOf(past), "a", today, unused)
        assertFalse(pinned.single().pinned)
    }

    @Test
    fun unpinningClearsTheOnlyPin() {
        val item = once("a", 2026, 12, 1, pinned = true)
        assertFalse(unpinOccasion(listOf(item), "a").single().pinned)
    }

    @Test
    fun yearlyTodayStaysPinned() {
        val today = LocalDate.of(2026, 5, 28)
        val item = yearly(5, 28).copy(id = "mom", title = "妈妈生日", pinned = true)
        val refreshed = refreshPins(listOf(item), today, unused)
        assertTrue(refreshed.single().pinned)
        assertEquals(
            "今天 · 妈妈生日",
            occasionLine(refreshed.single(), today.atTime(8, 0), unused),
        )
    }

    @Test
    fun futureBarCountsDaysThenHours() {
        val item = once("mom", 2026, 3, 28, pinned = true).copy(title = "妈妈生日")
        assertEquals(
            "距妈妈生日 86 天",
            occasionLine(item, LocalDateTime.of(2026, 1, 1, 0, 0), unused),
        )
        assertEquals(
            "距妈妈生日 5 小时",
            occasionLine(item, LocalDateTime.of(2026, 3, 27, 19, 0), unused),
        )
        assertEquals(
            "距妈妈生日 1 小时",
            occasionLine(item, LocalDateTime.of(2026, 3, 27, 23, 30), unused),
        )
    }

    @Test
    fun theBarKeepsTheCountApartFromTheTitle() {
        assertEquals(OccasionBarParts("距妈妈生日", " 86 天"), occasionBarParts("距妈妈生日 86 天"))
        assertEquals(OccasionBarParts("距妈妈生日", " 5 小时"), occasionBarParts("距妈妈生日 5 小时"))
        assertEquals(OccasionBarParts("距考试 2", " 1 小时"), occasionBarParts("距考试 2 1 小时"))
        assertEquals(OccasionBarParts("今天 · 妈妈生日", ""), occasionBarParts("今天 · 妈妈生日"))
    }

    @Test
    fun anUnpinnedItemDoesNotTakeTheBar() {
        val item = once("mom", 2026, 12, 1, pinned = false)
        assertNull(pinnedOccasionLine(listOf(item), LocalDateTime.of(2026, 1, 1, 0, 0), unused))
    }

    @Test
    fun titlesAndMessagesAreBounded() {
        assertNull(normalizeTitle("  "))
        assertNull(normalizeTitle("名".repeat(21)))
        assertEquals("妈妈生日", normalizeTitle(" 妈妈生日 "))
        assertEquals("到了", normalizeMessage("  "))
        assertEquals("a".repeat(20), normalizeMessage("a".repeat(25)))
    }

    @Test
    fun theThirtiethItemIsRefused() {
        val items = List(OCCASION_LIMIT) { index -> once("id$index", 2027, 1, 1) }
        assertNull(addOccasionItem(items, once("extra", 2027, 1, 2)))
        val added = addOccasionItem(listOf(once("a", 2027, 1, 1)), once("b", 2027, 1, 2))
        assertEquals(listOf("a", "b"), added?.map { it.id })
    }

    private fun once(id: String, year: Int, month: Int, day: Int, pinned: Boolean = false) = Occasion(
        id = id,
        title = "一次",
        kind = OccasionKind.SolarOnce,
        year = year,
        month = month,
        day = day,
        leap = false,
        message = "到了",
        pinned = pinned,
    )

    private fun yearly(month: Int, day: Int) = Occasion(
        id = "yearly",
        title = "每年",
        kind = OccasionKind.SolarYearly,
        year = null,
        month = month,
        day = day,
        leap = false,
        message = "到了",
        pinned = false,
    )

    private fun lunar(month: Int, day: Int, leap: Boolean) = Occasion(
        id = "lunar",
        title = "农历",
        kind = OccasionKind.LunarYearly,
        year = null,
        month = month,
        day = day,
        leap = leap,
        message = "到了",
        pinned = false,
    )
}

private fun script(
    yearOf: (LocalDate) -> Int = { it.year },
    solar: (Int, Int, Int, Boolean) -> LocalDate? = { _, _, _, _ -> null },
    leapOf: (Int) -> Int = { 0 },
    ever: (Int) -> Boolean = { false },
) = object : LunarCalendar {
    override fun lunarYear(date: LocalDate): Int = yearOf(date)
    override fun solarOf(lunarYear: Int, month: Int, day: Int, leap: Boolean): LocalDate? {
        return solar(lunarYear, month, day, leap)
    }
    override fun leapMonthOf(lunarYear: Int): Int = leapOf(lunarYear)
    override fun leapMonthEver(month: Int): Boolean = ever(month)
}
