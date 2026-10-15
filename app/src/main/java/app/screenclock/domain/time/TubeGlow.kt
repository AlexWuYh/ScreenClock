package app.screenclock.domain.time

const val TUBE_GLOW_MILLIS: Int = 700

/**
 * One glass-tube digit during a minute change.
 *
 * [brightness] is 0 at rest and 1 at the peak of the glow. Unchanged digits stay at 0.
 * [animate] false, a finished progress, or an unchanged group never glows.
 */
data class TubeGlyph(
    val char: Char,
    val brightness: Float,
)

fun tubeFrame(from: String, to: String, progress: Float, animate: Boolean): List<TubeGlyph> {
    val glow = if (!animate || from == to || progress >= 1f) {
        0f
    } else {
        tubePulse(progress)
    }
    return to.mapIndexed { index, char ->
        val changed = from.getOrNull(index) != char
        TubeGlyph(char = char, brightness = if (changed) glow else 0f)
    }
}

/** Rise to the middle of the glow, then fall. Not a loop. */
fun tubePulse(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return if (p <= 0.5f) p / 0.5f else (1f - p) / 0.5f
}
