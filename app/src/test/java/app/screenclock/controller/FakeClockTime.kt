package app.screenclock.controller

import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

/** A hand-cranked clock. Tests set [now] and [elapsed] before acting. */
internal class FakeClockTime(
    var now: LocalDateTime = LocalDateTime.of(2026, 10, 16, 10, 0),
    var nowMillis: Long = 1_000_000L,
    var elapsed: Long = 500_000L,
) : ClockTime {
    override fun now(): LocalDateTime = now
    override fun nowMillis(): Long = nowMillis
    override fun elapsedMillis(): Long = elapsed
}

/** Counts calls so tests can assert a controller stayed quiet. */
internal class Counter {
    val value = AtomicInteger(0)
    fun hit() {
        value.incrementAndGet()
    }
    fun get(): Int = value.get()
}
