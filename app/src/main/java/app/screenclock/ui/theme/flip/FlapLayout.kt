package app.screenclock.ui.theme.flip

const val FLAP_ASPECT: Float = 1.85f
const val FLAP_GAP_FRACTION: Float = 0.12f
const val FLAP_CORNER_FRACTION: Float = 0.08f

/** Portrait stacks the hour and minute groups. Landscape keeps them on one row. */
fun clockStacked(width: Float, height: Float): Boolean = height > width

data class FlapLayout(
    val cardWidth: Float,
    val cardHeight: Float,
    val gap: Float,
    val stacked: Boolean,
)

/**
 * Portrait stacks the two flaps. Landscape puts them in one row.
 * The gap stays a fraction of card width, including after the pair is scaled to fit.
 */
fun flapLayout(
    width: Float,
    height: Float,
    stacked: Boolean,
    reserveSide: Float,
): FlapLayout {
    require(width > 0f && height > 0f) { "flap area must be positive" }
    val side = reserveSide.coerceAtLeast(0f)
    val innerWidth = (width - side).coerceAtLeast(1f)
    return if (stacked) {
        var cardWidth = innerWidth
        var cardHeight = cardWidth / FLAP_ASPECT
        var gap = cardWidth * FLAP_GAP_FRACTION
        val totalHeight = cardHeight * 2f + gap
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
        val totalWidth = cardWidth * 2f + gap
        if (totalWidth > innerWidth) {
            val scale = innerWidth / totalWidth
            cardWidth *= scale
            cardHeight *= scale
            gap *= scale
        }
        FlapLayout(cardWidth, cardHeight, gap, stacked = false)
    }
}
