package app.screenclock.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.screenclock.ui.theme.digital.digitalStyle

/**
 * Hours, a colon and minutes as three separate pieces. The font's own colon sits low and
 * hugs its neighbours; here the colon is two dots drawn at the vertical middle of the digits,
 * with a clear gap on both sides. [outline] adds a thick border round digits and dots alike.
 *
 * [fontSize] is the size the caller would like. The pieces are measured, and when the whole
 * (digits, gaps, colon and outline) would not fit the width it is given, the size shrinks to
 * fit, so a wide face or a heavy outline can never push a digit off the screen.
 */
@Composable
internal fun SplitDigits(
    hour: String,
    minute: String,
    fontSize: TextUnit,
    color: Color,
    family: FontFamily,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    outline: Color? = null,
    style: TextStyle = digitalStyle(),
    gapFactor: Float = 0.12f,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val availablePx = constraints.maxWidth.toFloat()
        val wantedPx = with(density) { fontSize.toPx() }
        fun widthAt(px: Float): Float {
            val measureStyle = style.copy(fontFamily = family, fontWeight = weight, fontSize = with(density) { px.toSp() })
            val h = measurer.measure(AnnotatedString(hour), measureStyle, maxLines = 1, softWrap = false).size.width
            val m = measurer.measure(AnnotatedString(minute), measureStyle, maxLines = 1, softWrap = false).size.width
            // Two gaps, the colon, and the outline that spills past each side of each piece.
            val spill = if (outline != null) px * 0.13f else 0f
            return h + m + px * gapFactor * 2f + px * 0.16f + spill * 2f
        }
        val sizePx = if (availablePx.isFinite() && availablePx > 0f) {
            val whole = widthAt(wantedPx)
            if (whole > availablePx) wantedPx * (availablePx / whole) * 0.98f else wantedPx
        } else {
            wantedPx
        }
        val textSize = with(density) { sizePx.toSp() }
        val gap = with(density) { (sizePx * gapFactor).toDp() }
        val colonWidth = with(density) { (sizePx * 0.16f).toDp() }
        val colonHeight = with(density) { (sizePx * 0.5f).toDp() }
        val strokePx = sizePx * 0.13f
        @Composable
        fun part(text: String) {
            Box(contentAlignment = Alignment.Center) {
                if (outline != null) {
                    Text(
                        text = text, color = outline, fontSize = textSize, fontFamily = family, fontWeight = weight,
                        maxLines = 1, softWrap = false,
                        style = style.copy(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)),
                    )
                }
                Text(
                    text = text, color = color, fontSize = textSize, fontFamily = family, fontWeight = weight,
                    maxLines = 1, softWrap = false, style = style,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            part(hour)
            Spacer(Modifier.size(gap))
            Canvas(Modifier.size(colonWidth, colonHeight)) {
                val r = this.size.width / 2f
                for (y in listOf(this.size.height * 0.16f, this.size.height * 0.84f)) {
                    if (outline != null) drawCircle(outline, r + strokePx / 2f, Offset(this.size.width / 2f, y))
                }
                for (y in listOf(this.size.height * 0.16f, this.size.height * 0.84f)) {
                    drawCircle(color, r, Offset(this.size.width / 2f, y))
                }
            }
            Spacer(Modifier.size(gap))
            part(minute)
        }
    }
}
