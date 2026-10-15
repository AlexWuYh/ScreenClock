package app.screenclock.domain.bedside

const val NIGHT_START_DEFAULT: Int = 22

const val NIGHT_END_DEFAULT: Int = 7

data class BedsidePreferences(
    val dimEnabled: Boolean = false,
    val startHour: Int = NIGHT_START_DEFAULT,
    val endHour: Int = NIGHT_END_DEFAULT,
    val warmEnabled: Boolean = false,
    val chargeKeepAwake: Boolean = false,
)

/** Stored hours outside 0–23 fall back to the bedside default for that field. */
fun normalizeHour(value: Int, default: Int): Int {
    return if (value in 0..23) value else default
}

fun bedsidePreferences(
    dimEnabled: Boolean,
    startHour: Int,
    endHour: Int,
    warmEnabled: Boolean,
    chargeKeepAwake: Boolean,
): BedsidePreferences {
    return BedsidePreferences(
        dimEnabled = dimEnabled,
        startHour = normalizeHour(startHour, NIGHT_START_DEFAULT),
        endHour = normalizeHour(endHour, NIGHT_END_DEFAULT),
        warmEnabled = warmEnabled,
        chargeKeepAwake = chargeKeepAwake,
    )
}

/**
 * Whether this window should dim.
 * [minuteOfDay] is local minutes from midnight.
 * [endHour] is exclusive. The window wraps midnight when the start hour is later.
 * Equal hours are an empty window. Disabled never dims.
 */
fun nightDimActive(enabled: Boolean, minuteOfDay: Int, startHour: Int, endHour: Int): Boolean {
    if (!enabled) return false
    if (minuteOfDay !in 0 until 24 * 60) return false
    val start = normalizeHour(startHour, NIGHT_START_DEFAULT)
    val end = normalizeHour(endHour, NIGHT_END_DEFAULT)
    if (start == end) return false
    val startMinute = start * 60
    val endMinute = end * 60
    return if (start < end) {
        minuteOfDay in startMinute until endMinute
    } else {
        minuteOfDay >= startMinute || minuteOfDay < endMinute
    }
}

/** Warm tint is stored on its own, and only shows while the night window is dimming. */
fun warmActive(nightActive: Boolean, warmEnabled: Boolean): Boolean {
    return nightActive && warmEnabled
}

/**
 * One window flag. Focus holds the screen only during a countdown or the done face.
 * Charge holds it only when the switch is on and the device is plugged in.
 */
fun keepScreenOn(focusHolding: Boolean, chargeEnabled: Boolean, plugged: Boolean): Boolean {
    return focusHolding || (chargeEnabled && plugged)
}
