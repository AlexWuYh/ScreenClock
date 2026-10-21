package app.screenclock.ui.theme.flip

const val FLAP_ASPECT: Float = 1.05f
const val FLAP_GAP_FRACTION: Float = 0.08f
const val FLAP_CORNER_FRACTION: Float = 0.08f

/** A readable split on phones and tablets without scaling into an oversized stripe. */
internal fun hingeStrokePx(cardHeightPx: Float, minStrokePx: Float, maxStrokePx: Float): Float =
    (cardHeightPx * 0.012f).coerceIn(minStrokePx, maxStrokePx)

/** Flap groups always share one row; the hinge is what splits top from bottom. */
fun clockStacked(width: Float, height: Float): Boolean = false

data class FlapLayout(
    val cardWidth: Float,
    val cardHeight: Float,
    val gap: Float,
    val stacked: Boolean,
)

/**
 * Production clock groups share one row in every orientation; the optional stacked
 * branch remains for callers of [flapLayout] that request it explicitly.
 * [count] is 2 for the home clock, and 2 or 3 for a countdown.
 * The gap stays a fraction of card width, including after the row is scaled to fit.
 */
fun flapLayout(
    width: Float,
    height: Float,
    stacked: Boolean,
    reserveSide: Float,
    count: Int = 2,
): FlapLayout {
    require(width > 0f && height > 0f) { "flap area must be positive" }
    require(count >= 1) { "flap count must be positive" }
    val side = reserveSide.coerceAtLeast(0f)
    val innerWidth = (width - side).coerceAtLeast(1f)
    val gaps = (count - 1).coerceAtLeast(0)
    return if (stacked) {
        var cardWidth = innerWidth
        var cardHeight = cardWidth / FLAP_ASPECT
        var gap = cardWidth * FLAP_GAP_FRACTION
        val totalHeight = cardHeight * count + gap * gaps
        if (totalHeight > height) {
            val scale = height / totalHeight
            cardWidth *= scale
            cardHeight *= scale
            gap *= scale
        }
        FlapLayout(cardWidth, cardHeight, gap, stacked = true)
    } else {
        var cardHeight = height.coerceAtLeast(1f)
        var cardWidth = cardHeight * FLAP_ASPECT
        var gap = cardWidth * FLAP_GAP_FRACTION
        val totalWidth = cardWidth * count + gap * gaps
        if (totalWidth > innerWidth) {
            val scale = innerWidth / totalWidth
            cardWidth *= scale
            cardHeight *= scale
            gap *= scale
        }
        FlapLayout(cardWidth, cardHeight, gap, stacked = false)
    }
}
