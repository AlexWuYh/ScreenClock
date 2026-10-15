package app.screenclock.domain.time

const val FLIP_DURATION_MILLIS: Int = 400

/**
 * One flap, two stages.
 *
 * The first half of [progress] folds the old top down and reveals the new top.
 * The second half folds the new bottom up. [animate] false, or a finished
 * progress, is the new digits with nothing moving.
 */
data class FlipFrame(
    val topBack: String,
    val topFlap: String?,
    val topFlapTurns: Float,
    val bottomBack: String,
    val bottomFlap: String?,
    val bottomFlapTurns: Float,
)

fun flipFrame(from: String, to: String, progress: Float, animate: Boolean): FlipFrame {
    if (!animate || from == to || progress >= 1f) {
        return settled(to)
    }
    val p = progress.coerceIn(0f, 1f)
    return if (p < 0.5f) {
        FlipFrame(
            topBack = to,
            topFlap = from,
            topFlapTurns = p / 0.5f,
            bottomBack = from,
            bottomFlap = null,
            bottomFlapTurns = 0f,
        )
    } else {
        FlipFrame(
            topBack = to,
            topFlap = null,
            topFlapTurns = 1f,
            bottomBack = from,
            bottomFlap = to,
            bottomFlapTurns = (p - 0.5f) / 0.5f,
        )
    }
}

private fun settled(text: String): FlipFrame {
    return FlipFrame(
        topBack = text,
        topFlap = null,
        topFlapTurns = 0f,
        bottomBack = text,
        bottomFlap = null,
        bottomFlapTurns = 0f,
    )
}
