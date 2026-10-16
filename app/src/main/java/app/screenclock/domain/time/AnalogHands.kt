package app.screenclock.domain.time

/**
 * Where the two hands point. The home screen keeps seconds out of it,
 * so the hour hand only creeps with the minute.
 */
data class HandAngles(
    val hourDegrees: Float,
    val minuteDegrees: Float,
)

/** 0° points straight up and the hands run clockwise, as dials do. */
fun handAngles(hour: Int, minute: Int): HandAngles {
    val h = ((hour % 12) + 12) % 12
    val m = ((minute % 60) + 60) % 60
    val minuteDegrees = m * 6f
    val hourDegrees = (h * 30f) + (m * 0.5f)
    return HandAngles(hourDegrees = hourDegrees, minuteDegrees = minuteDegrees)
}
