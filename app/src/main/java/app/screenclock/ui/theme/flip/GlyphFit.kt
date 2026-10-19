package app.screenclock.ui.theme.flip

/** Ink stays inside the flap and clear of the rounded corners. */
internal const val GLYPH_WIDTH_FRACTION: Float = 0.96f
internal const val GLYPH_HEIGHT_FRACTION: Float = 0.92f

internal data class GlyphFit(
    val sizeScale: Float,
    val inkLeft: Float,
    val inkTop: Float,
)

/**
 * Scales a glyph measured at [measuredSize] so its ink fits in the card.
 * [inkLeft] and [inkTop] are the top-left of that scaled ink.
 */
internal fun glyphFit(
    cardWidth: Float,
    cardHeight: Float,
    inkWidth: Float,
    inkHeight: Float,
    measuredSize: Float,
): GlyphFit {
    require(cardWidth > 0f && cardHeight > 0f) { "card must be positive" }
    require(inkWidth > 0f && inkHeight > 0f && measuredSize > 0f) { "ink must be positive" }
    val maxWidth = cardWidth * GLYPH_WIDTH_FRACTION
    val maxHeight = cardHeight * GLYPH_HEIGHT_FRACTION
    val sizeScale = minOf(maxWidth / inkWidth, maxHeight / inkHeight)
    val drawnWidth = inkWidth * sizeScale
    val drawnHeight = inkHeight * sizeScale
    return GlyphFit(
        sizeScale = sizeScale,
        inkLeft = (cardWidth - drawnWidth) / 2f,
        inkTop = (cardHeight - drawnHeight) / 2f,
    )
}
