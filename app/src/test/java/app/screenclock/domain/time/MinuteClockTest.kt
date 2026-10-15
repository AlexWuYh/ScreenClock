package app.screenclock.domain.time

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MinuteClockTest {
    @Test
    fun displayMinuteDropsSeconds() {
        val shown = displayMinute(LocalDateTime.of(2026, 10, 14, 10, 5, 42, 900_000_000))
        assertEquals(LocalDateTime.of(2026, 10, 14, 10, 5, 0), shown)
    }

    @Test
    fun waitAtTheMinuteBoundaryIsOneMinute() {
        val atBoundary = LocalDateTime.of(2026, 10, 14, 10, 0, 0, 0)
        assertEquals(60_000L, millisUntilNextMinute(atBoundary))
    }

    @Test
    fun waitIsTheRemainderUntilTheNextMinute() {
        val mid = LocalDateTime.of(2026, 10, 14, 10, 0, 30, 0)
        assertEquals(30_000L, millisUntilNextMinute(mid))
        val almost = LocalDateTime.of(2026, 10, 14, 10, 0, 59, 999_000_000)
        val wait = millisUntilNextMinute(almost)
        assertTrue(wait in 1L..1_000L)
    }
}
