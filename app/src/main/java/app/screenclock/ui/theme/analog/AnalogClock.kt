package app.screenclock.ui.theme.analog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.handAngles
import app.screenclock.ui.Cream
import app.screenclock.ui.Digit
import app.screenclock.ui.Ink
import app.screenclock.ui.theme.ClockScaffold
import kotlin.math.cos
import kotlin.math.sin

/**
 * A quiet dial. Only hour and minute hands: the home screen has no seconds,
 * and the hands step once per minute like everything else on the face.
 */
@Composable
internal fun AnalogClock(
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
        val diameter = minOf(areaWidth, areaHeight)
        val hour = face.hourDigits.toIntOrNull() ?: 0
        val minute = face.minuteDigits.toIntOrNull() ?: 0
        val angles = handAngles(hour, minute)
        Canvas(modifier = Modifier.size(diameter, diameter)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = Cream.copy(alpha = 0.22f),
                radius = r * 0.98f,
                center = center,
                style = Stroke(width = r * 0.012f),
            )
            for (tick in 0 until 60) {
                val isHour = tick % 5 == 0
                val length = if (isHour) r * 0.10f else r * 0.045f
                val alpha = if (isHour) 0.85f else 0.28f
                val angle = Math.toRadians((tick * 6).toDouble())
                val outer = r * 0.92f
                val inner = outer - length
                drawLine(
                    color = Cream.copy(alpha = alpha),
                    start = center + Offset((sin(angle) * inner).toFloat(), (-cos(angle) * inner).toFloat()),
                    end = center + Offset((sin(angle) * outer).toFloat(), (-cos(angle) * outer).toFloat()),
                    strokeWidth = r * (if (isHour) 0.014f else 0.008f),
                    cap = StrokeCap.Round,
                )
            }
            drawHand(center, r, angles.hourDegrees, length = r * 0.52f, width = r * 0.058f, color = Cream)
            drawHand(center, r, angles.minuteDegrees, length = r * 0.82f, width = r * 0.038f, color = Digit)
            drawCircle(color = Cream, radius = r * 0.045f, center = center)
            drawCircle(color = Ink, radius = r * 0.018f, center = center)
        }
    }
}

private fun DrawScope.drawHand(
    center: Offset,
    radius: Float,
    degrees: Float,
    length: Float,
    width: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    rotate(degrees = degrees, pivot = center) {
        drawLine(
            color = color,
            start = center + Offset(0f, radius * 0.12f),
            end = center + Offset(0f, -length),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
}
