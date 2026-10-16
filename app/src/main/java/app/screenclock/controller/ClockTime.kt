package app.screenclock.controller

import java.time.LocalDateTime

/**
 * The three clocks the app reads. Controllers take one of these instead of calling
 * the system directly, so tests can drive time.
 */
interface ClockTime {
    /** Wall clock, for dates, countdowns and previews. */
    fun now(): LocalDateTime

    /** Wall clock in millis, for focus ends and weather timestamps. */
    fun nowMillis(): Long

    /** Monotonic millis, for burn-in phases. Unaffected by wall clock jumps. */
    fun elapsedMillis(): Long
}
