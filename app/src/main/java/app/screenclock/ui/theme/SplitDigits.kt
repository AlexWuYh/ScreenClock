package app.screenclock.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import app.screenclock.ui.theme.digital.digitalStyle

/**
 * Hours, a colon and minutes as three separate pieces. The font's own colon sits low and
 * hugs its neighbours; here the colon is two dots drawn at the vertical middle of the digits,
 * with a clear gap on both sides. [outline] adds a thick border round digits and dots alike.
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
    gapFactor: Float = 0.2f,
) {
    val density = LocalDensity.current
    val sizePx = with(density) { fontSize.toPx() }
    val gap = with(density) { (sizePx * gapFactor).toDp() }
    val colonWidth = with(density) { (sizePx * 0.16f).toDp() }
    val colonHeight = with(density) { (sizePx * 0.5f).toDp() }
    val strokePx = sizePx * 0.13f
    @Composable
    fun part(text: String) {
        Box(contentAlignment = Alignment.Center) {
            if (outline != null) {
                Text(
                    text = text, color = outline, fontSize = fontSize, fontFamily = family, fontWeight = weight,
                    style = style.copy(drawStyle = Stroke(width = strokePx, join = StrokeJoin.Round)),
                )
            }
            Text(text = text, color = color, fontSize = fontSize, fontFamily = family, fontWeight = weight, style = style)
        }
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        part(hour)
        androidx.compose.foundation.layout.Spacer(Modifier.size(gap))
        Canvas(Modifier.size(colonWidth, colonHeight)) {
            val r = size.width / 2f
            for (y in listOf(size.height * 0.16f, size.height * 0.84f)) {
                val c = Offset(size.width / 2f, y)
                if (outline != null) drawCircle(outline, r + strokePx / 2f, c)
            }
            for (y in listOf(size.height * 0.16f, size.height * 0.84f)) {
                drawCircle(color, r, Offset(size.width / 2f, y))
            }
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(gap))
        part(minute)
    }
}
