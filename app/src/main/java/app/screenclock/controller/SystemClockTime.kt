package app.screenclock.controller

import android.os.SystemClock
import java.time.LocalDateTime

/** The production clocks. Burn-in uses uptime so a time change cannot fake an idle period. */
object SystemClockTime : ClockTime {
    override fun now(): LocalDateTime = LocalDateTime.now()

    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun elapsedMillis(): Long = SystemClock.elapsedRealtime()
}
