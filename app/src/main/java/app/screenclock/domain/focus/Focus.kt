package app.screenclock.domain.focus

const val FOCUS_MIN_MINUTES: Int = 1
const val FOCUS_MAX_MINUTES: Int = 180
const val FOCUS_PRESET_SHORT_MINUTES: Int = 25
const val FOCUS_PRESET_LONG_MINUTES: Int = 50
const val FOCUS_HOLD_MILLIS: Long = 600L

private const val MINUTE_MILLIS: Long = 60_000L

enum class FocusPhase {
    Running,
    Done,
}

/** Two groups are minutes and seconds. Three groups are hours, minutes, and seconds. */
data class FocusFace(
    val phase: FocusPhase,
    val groups: List<String>,
)

fun normalizeFocusMinutes(minutes: Int): Int {
    return minutes.coerceIn(FOCUS_MIN_MINUTES, FOCUS_MAX_MINUTES)
}

fun focusEndEpochMillis(nowEpochMillis: Long, minutes: Int): Long {
    return nowEpochMillis + normalizeFocusMinutes(minutes).toLong() * MINUTE_MILLIS
}

/** Missing or non-positive stored values are not a session. */
fun storedFocusEnd(raw: Long?): Long? {
    if (raw == null || raw <= 0L) return null
    return raw
}

/**
 * Remaining time is [endEpochMillis] minus [nowEpochMillis], and never below zero.
 * Whole seconds round up, so the first instant of a minute still shows that minute.
 * A full hour uses three groups. Under an hour uses two.
 */
fun focusFace(endEpochMillis: Long, nowEpochMillis: Long): FocusFace {
    val remaining = (endEpochMillis - nowEpochMillis).coerceAtLeast(0L)
    if (remaining == 0L) {
        return FocusFace(FocusPhase.Done, listOf("00", "00"))
    }
    val totalSeconds = (remaining + 999L) / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val groups = if (hours >= 1L) {
        listOf(twoDigits(hours), twoDigits(minutes), twoDigits(seconds))
    } else {
        listOf(twoDigits(minutes), twoDigits(seconds))
    }
    return FocusFace(FocusPhase.Running, groups)
}

/** Milliseconds until the rounded second changes. Null once the session has reached zero. */
fun millisUntilFocusDigitChange(endEpochMillis: Long, nowEpochMillis: Long): Long? {
    val remaining = endEpochMillis - nowEpochMillis
    if (remaining <= 0L) return null
    val into = remaining % 1000L
    return if (into == 0L) 1000L else into
}

fun focusSpoken(face: FocusFace): String {
    if (face.phase == FocusPhase.Done) return "专注结束"
    return if (face.groups.size >= 3) {
        val hours = face.groups[0].toInt()
        "剩余${hours}小时${face.groups[1]}分${face.groups[2]}秒"
    } else {
        "剩余${face.groups[0]}分${face.groups[1]}秒"
    }
}

private fun twoDigits(value: Long): String {
    return if (value > 99L) value.toString() else value.toString().padStart(2, '0')
}
