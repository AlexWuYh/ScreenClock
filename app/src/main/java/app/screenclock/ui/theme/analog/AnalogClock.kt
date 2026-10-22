package app.screenclock.ui.theme.analog

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.handAngles
import app.screenclock.ui.SceneLight
import app.screenclock.ui.theme.ClockScaffold
import app.screenclock.ui.theme.ThemeFont
import app.screenclock.ui.theme.rememberThemeTypeface
import app.screenclock.ui.theme.scene.MountainScene
import kotlin.math.cos
import kotlin.math.sin

private val DialFace = Color(0xFFF5F2EA)
private val DialInk = Color(0xFF2E343B)

/**
 * 经典模拟：a numbered dial with thin hands over drawn snow ridges.
 * No second hand: the home screen steps once per minute.
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
    val themeFont = rememberThemeTypeface(ThemeFont.Analog)
    Box(modifier = modifier) {
        MountainScene(modifier = Modifier.fillMaxSize())
        ClockScaffold(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
            preview = preview,
            infoColor = SceneLight.copy(alpha = 0.9f),
            onOccasionClick = onOccasionClick,
        ) { stacked, cardWidth, cardHeight, gap ->
            val areaWidth = if (stacked) cardWidth else cardWidth * 2 + gap
            val areaHeight = if (stacked) cardHeight * 2 + gap else cardHeight
            val diameter = minOf(areaWidth, areaHeight) * 0.96f
            val hour = face.hourDigits.toIntOrNull() ?: 0
            val minute = face.minuteDigits.toIntOrNull() ?: 0
            val angles = handAngles(hour, minute)
            Canvas(modifier = Modifier.requiredSize(diameter, diameter)) {
                val r = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = DialFace, radius = r * 0.99f, center = center)
                drawCircle(
                    color = Color.White.copy(alpha = 0.75f),
                    radius = r * 0.94f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.009f),
                )
                for (tick in 0 until 60) {
                    val isHour = tick % 5 == 0
                    val angle = Math.toRadians((tick * 6).toDouble())
                    val outer = r * 0.97f
                    val inner = outer - if (isHour) r * 0.075f else r * 0.035f
                    drawLine(
                        color = DialInk.copy(alpha = if (isHour) 0.86f else 0.36f),
                        start = center + Offset((sin(angle) * inner).toFloat(), (-cos(angle) * inner).toFloat()),
                        end = center + Offset((sin(angle) * outer).toFloat(), (-cos(angle) * outer).toFloat()),
                        strokeWidth = r * (if (isHour) 0.012f else 0.006f),
                        cap = StrokeCap.Round,
                    )
                }
                val numeralPaint = Paint().apply {
                    isAntiAlias = true
                    color = DialInk.copy(alpha = 0.88f).toArgb()
                    textAlign = Paint.Align.CENTER
                    typeface = themeFont
                    textSize = r * 0.36f
                }
                for (number in 1..12) {
                    val angle = Math.toRadians((number * 30).toDouble())
                    val x = center.x + (sin(angle) * r * 0.72f).toFloat()
                    val y = center.y - (cos(angle) * r * 0.72f).toFloat()
                    drawContext.canvas.nativeCanvas.drawText(
                        number.toString(),
                        x,
                        y + numeralPaint.textSize * 0.35f,
                        numeralPaint,
                    )
                }
                drawHand(center, r, angles.hourDegrees, length = r * 0.42f, width = r * 0.030f, color = DialInk)
                drawHand(center, r, angles.minuteDegrees, length = r * 0.62f, width = r * 0.022f, color = DialInk)
                drawCircle(color = DialInk, radius = r * 0.035f, center = center)
                drawCircle(color = Color(0xFFBE8960), radius = r * 0.014f, center = center)
            }
        }
    }
}

private fun DrawScope.drawHand(
    center: Offset,
    radius: Float,
    degrees: Float,
    length: Float,
    width: Float,
    color: Color,
) {
    rotate(degrees = degrees, pivot = center) {
        drawLine(
            color = color,
            start = center + Offset(0f, radius * 0.10f),
            end = center + Offset(0f, -length),
            strokeWidth = width,
            cap = StrokeCap.Round,
        )
    }
}
