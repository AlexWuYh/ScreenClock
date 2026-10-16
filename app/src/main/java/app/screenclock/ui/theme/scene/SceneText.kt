package app.screenclock.ui.theme.scene

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A text size that keeps a five-glyph time string (「17:42」) inside the digits
 * area: height-driven on wide canvases, width-driven on tall narrow ones.
 */
@Composable
internal fun sceneDigitSize(areaWidth: Dp, areaHeight: Dp, widthGlyphs: Float = 2.75f): TextUnit {
    val density = LocalDensity.current
    val heightSp = with(density) { areaHeight.toSp() }.value
    val widthSp = with(density) { areaWidth.toSp() }.value
    return minOf(heightSp * 0.62f, widthSp / widthGlyphs).sp
}
