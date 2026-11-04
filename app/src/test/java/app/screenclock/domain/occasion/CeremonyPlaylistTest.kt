package app.screenclock.domain.occasion

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CeremonyPlaylistTest {
    private val today = LocalDate.of(2026, 11, 4)
    private val unused = object : LunarCalendar {
        override fun lunarYear(date: LocalDate): Int = error("unused")
        override fun solarOf(lunarYear: Int, month: Int, day: Int, leap: Boolean): LocalDate? = error("unused")
        override fun leapMonthOf(lunarYear: Int): Int = error("unused")
        override fun leapMonthEver(month: Int): Boolean = error("unused")
    }

    private fun once(id: String, year: Int, month: Int, day: Int) = Occasion(
        id = id, title = id, kind = OccasionKind.SolarOnce, year = year, month = month, day = day,
        leap = false, message = "", pinned = false,
    )

    @Test
    fun nearestDateComesFirstAndPassedItemsAreLeftOut() {
        val items = listOf(
            once("late", 2027, 6, 1),
            once("past", 2026, 1, 1),
            once("soon", 2026, 12, 25),
            once("today", 2026, 11, 4),
        )
        assertEquals(listOf("today", "soon", "late"), ceremonyPlaylist(items, today, unused).map { it.id })
    }

    @Test
    fun equalDatesKeepListOrder() {
        val items = listOf(once("b", 2027, 1, 1), once("a", 2027, 1, 1))
        assertEquals(listOf("b", "a"), ceremonyPlaylist(items, today, unused).map { it.id })
    }

    @Test
    fun yearlyItemUsesItsNextDate() {
        val yearly = Occasion(
            id = "y", title = "y", kind = OccasionKind.SolarYearly, year = null, month = 1, day = 1,
            leap = false, message = "", pinned = false,
        )
        val stops = ceremonyPlaylist(listOf(once("far", 2028, 1, 1), yearly), today, unused)
        assertEquals(listOf("y", "far"), stops.map { it.id })
        assertEquals(LocalDate.of(2027, 1, 1), stops.first().occurrence)
    }

    @Test
    fun aRunEndsAfterTheLastDate() {
        assertEquals(1, nextStopIndex(0, 3))
        assertEquals(2, nextStopIndex(1, 3))
        assertNull(nextStopIndex(2, 3))
        assertNull(nextStopIndex(0, 0))
    }

    @Test
    fun autoPlayStayDefaultsToTenSecondsAndOnlyAcceptsTheOffered() {
        assertEquals(10, CeremonyLook().autoPlaySeconds)
        assertEquals(10, normalizeAutoPlaySeconds(null))
        assertEquals(10, normalizeAutoPlaySeconds(7))
        assertEquals(10, normalizeAutoPlaySeconds(0))
        assertEquals(60, normalizeAutoPlaySeconds(60))
        assertEquals(5, normalizeAutoPlaySeconds(5))
    }
}
