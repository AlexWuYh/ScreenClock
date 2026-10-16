package app.screenclock.ui.theme.moon

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.Digit
import app.screenclock.ui.Ink
import app.screenclock.ui.MoonGrey
import app.screenclock.ui.theme.ClockScaffold

/**
 * Outlined digits in front of a drawn moon. The moon is carved by the page colour,
 * so nothing but the app's own shapes ever reaches the screen.
 */
@Composable
internal fun MoonClock(
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
        val text = "${face.hourDigits}:${face.minuteDigits}"
        Canvas(modifier = Modifier.size(areaWidth, areaHeight)) {
            val w = size.width
            val h = size.height
            val moonRadius = h * 0.44f
            val moonCenter = Offset(w - moonRadius * 1.1f, h * 0.5f)
            drawCircle(color = MoonGrey.copy(alpha = 0.8f), radius = moonRadius, center = moonCenter)
            drawCircle(
                color = MoonGrey.copy(alpha = 0.35f),
                radius = moonRadius * 0.16f,
                center = moonCenter + Offset(-moonRadius * 0.28f, -moonRadius * 0.3f),
            )
            drawCircle(
                color = MoonGrey.copy(alpha = 0.3f),
                radius = moonRadius * 0.11f,
                center = moonCenter + Offset(moonRadius * 0.3f, moonRadius * 0.12f),
            )
            drawCircle(
                color = MoonGrey.copy(alpha = 0.28f),
                radius = moonRadius * 0.08f,
                center = moonCenter + Offset(-moonRadius * 0.05f, moonRadius * 0.42f),
            )
            // A waning crescent: the page colour eats into the disc from the left.
            drawCircle(color = Ink, radius = moonRadius * 1.02f, center = moonCenter + Offset(-moonRadius * 0.52f, -moonRadius * 0.14f))

            val paint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = (h * 0.016f).coerceAtLeast(1.5f)
                color = Digit.toArgb()
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                textSize = h * 0.6f
            }
            val bounds = Rect()
            paint.getTextBounds(text, 0, text.length, bounds)
            val x = (w - paint.measureText(text)) * 0.42f
            val baseline = h / 2f - bounds.exactCenterY()
            drawContext.canvas.nativeCanvas.drawText(text, x, baseline, paint)
        }
    }
}
