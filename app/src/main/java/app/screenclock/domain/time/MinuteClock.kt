package app.screenclock.domain.time

import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Drop seconds so the home clock never changes between minutes. */
fun displayMinute(now: LocalDateTime): LocalDateTime {
    return now.truncatedTo(ChronoUnit.MINUTES)
}

/** Wait until the next minute boundary. Never a one-second tick. */
fun millisUntilNextMinute(now: LocalDateTime): Long {
    val next = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
    return Duration.between(now, next).toMillis().coerceAtLeast(1L)
}
