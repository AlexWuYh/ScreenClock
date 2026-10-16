package app.screenclock.ui.theme.lcd

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.LcdSegment
import app.screenclock.domain.time.lcdSegments
import app.screenclock.ui.LcdInk
import app.screenclock.ui.LcdPanel
import app.screenclock.ui.theme.ClockScaffold

private val Ghost: Color = LcdInk.copy(alpha = 0.055f)

/**
 * A desk-clock panel: light body, dark seven-segment digits, and the ghost of
 * every unlit segment so the glass reads as an LCD.
 */
@Composable
internal fun LcdClock(
    face: FaceTime,
    dateLine: String?,
    weatherLine: String?,
    occasionLine: String?,
    expanded: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
    onOccasionClick: (() -> Unit)? = null,
) {
    ClockScaffold(
        face = face,
        dateLine = dateLine,
        weatherLine = weatherLine,
        occasionLine = occasionLine,
        expanded = expanded,
        modifier = modifier,
        preview = preview,
        onOccasionClick = onOccasionClick,
    ) { stacked, cardWidth, cardHeight, gap ->
        val areaWidth = if (stacked) cardWidth else cardWidth * 2 + gap
        val areaHeight = if (stacked) cardHeight * 2 + gap else cardHeight
        val digits = face.hourDigits + face.minuteDigits
        Canvas(modifier = Modifier.size(areaWidth, areaHeight)) {
            val w = size.width
            val h = size.height
            val corner = minOf(w, h) * 0.16f
            drawRoundRect(
                color = LcdPanel,
                cornerRadius = CornerRadius(corner, corner),
            )
            // Four digits around a colon, kept inside the middle band of the panel.
            val groupWidth = w * 0.78f
            val digitHeight = h * 0.52f
            val digitWidth = digitHeight * 0.58f
            val colonWidth = (groupWidth - digitWidth * 4f).coerceAtLeast(w * 0.03f)
            val startX = (w - (digitWidth * 4f + colonWidth)) / 2f
            val topY = (h - digitHeight) / 2f
            for (index in 0 until 4) {
                val offset = if (index < 2) 0f else colonWidth
                drawLcdDigit(
                    left = startX + digitWidth * index + offset,
                    top = topY,
                    width = digitWidth,
                    height = digitHeight,
                    on = lcdSegments(digits[index]),
                )
            }
            val colonX = startX + digitWidth * 2f + colonWidth / 2f
            val dot = digitWidth * 0.16f
            drawRoundRect(
                color = LcdInk,
                topLeft = Offset(colonX - dot / 2f, topY + digitHeight * 0.26f - dot / 2f),
                size = Size(dot, dot),
                cornerRadius = CornerRadius(dot / 2f, dot / 2f),
            )
            drawRoundRect(
                color = LcdInk,
                topLeft = Offset(colonX - dot / 2f, topY + digitHeight * 0.74f - dot / 2f),
                size = Size(dot, dot),
                cornerRadius = CornerRadius(dot / 2f, dot / 2f),
            )
        }
    }
}

private fun DrawScope.drawLcdDigit(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    on: Set<LcdSegment>,
) {
    val t = width * 0.19f
    val middle = top + height / 2f

    fun horizontal(y: Float, enabled: Boolean) {
        drawRoundRect(
            color = if (enabled) LcdInk else Ghost,
            topLeft = Offset(left + t * 1.2f, y - t / 2f),
            size = Size(width - t * 2.4f, t),
            cornerRadius = CornerRadius(t / 2f, t / 2f),
        )
    }

    fun vertical(x: Float, y: Float, enabled: Boolean) {
        drawRoundRect(
            color = if (enabled) LcdInk else Ghost,
            topLeft = Offset(left + x - t / 2f, y),
            size = Size(t, height / 2f - t * 1.9f),
            cornerRadius = CornerRadius(t / 2f, t / 2f),
        )
    }

    horizontal(top + t / 2f, LcdSegment.Top in on)
    horizontal(middle, LcdSegment.Middle in on)
    horizontal(top + height - t / 2f, LcdSegment.Bottom in on)
    vertical(t, top + t * 1.1f, LcdSegment.TopLeft in on)
    vertical(width - t, top + t * 1.1f, LcdSegment.TopRight in on)
    vertical(t, middle + t * 0.9f, LcdSegment.BottomLeft in on)
    vertical(width - t, middle + t * 0.9f, LcdSegment.BottomRight in on)
}
