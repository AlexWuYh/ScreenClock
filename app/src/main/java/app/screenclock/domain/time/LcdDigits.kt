package app.screenclock.domain.time

/** The seven segments of one LCD digit, top bar clockwise then the middle bar. */
enum class LcdSegment {
    Top,
    TopRight,
    BottomRight,
    Bottom,
    BottomLeft,
    TopLeft,
    Middle,
}

/**
 * Which segments light up for a digit. Unknown characters light nothing,
 * so a missing glyph never invents a shape.
 */
fun lcdSegments(char: Char): Set<LcdSegment> {
    val all = LcdSegment.entries.toSet()
    return when (char) {
        '0' -> all - LcdSegment.Middle
        '1' -> setOf(LcdSegment.TopRight, LcdSegment.BottomRight)
        '2' -> setOf(
            LcdSegment.Top,
            LcdSegment.TopRight,
            LcdSegment.Middle,
            LcdSegment.BottomLeft,
            LcdSegment.Bottom,
        )
        '3' -> setOf(
            LcdSegment.Top,
            LcdSegment.TopRight,
            LcdSegment.Middle,
            LcdSegment.BottomRight,
            LcdSegment.Bottom,
        )
        '4' -> setOf(
            LcdSegment.TopLeft,
            LcdSegment.Middle,
            LcdSegment.TopRight,
            LcdSegment.BottomRight,
        )
        '5' -> setOf(
            LcdSegment.Top,
            LcdSegment.TopLeft,
            LcdSegment.Middle,
            LcdSegment.BottomRight,
            LcdSegment.Bottom,
        )
        '6' -> setOf(
            LcdSegment.Top,
            LcdSegment.TopLeft,
            LcdSegment.Middle,
            LcdSegment.BottomLeft,
            LcdSegment.Bottom,
            LcdSegment.BottomRight,
        )
        '7' -> setOf(LcdSegment.Top, LcdSegment.TopRight, LcdSegment.BottomRight)
        '8' -> all
        '9' -> setOf(
            LcdSegment.Top,
            LcdSegment.TopRight,
            LcdSegment.BottomRight,
            LcdSegment.Bottom,
            LcdSegment.TopLeft,
            LcdSegment.Middle,
        )
        '-' -> setOf(LcdSegment.Middle)
        else -> emptySet()
    }
}
