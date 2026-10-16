package app.screenclock.ui.theme.neon

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.theme.ClockScaffold
import app.screenclock.ui.theme.scene.NeonScene

private val MeridiemTint = androidx.compose.ui.graphics.Color(0xFFD9A6E8)

/**
 * 复古霓虹：gradient neon digits on a synthwave grid. The neon is a stroke
 * shader with a few glow passes under the bright outline.
 */
@Composable
internal fun NeonClock(
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
    Box(modifier = modifier) {
        NeonScene(modifier = Modifier.fillMaxSize())
        ClockScaffold(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
            preview = preview,
            meridiemBelow = true,
            infoColor = MeridiemTint,
            onOccasionClick = onOccasionClick,
        ) { stacked, cardWidth, cardHeight, gap ->
            val areaWidth = if (stacked) cardWidth else cardWidth * 2 + gap
            val areaHeight = if (stacked) cardHeight * 2 + gap else cardHeight
            val text = "${face.hourDigits}:${face.minuteDigits}"
            Canvas(modifier = Modifier.size(areaWidth, areaHeight)) {
                val w = size.width
                val h = size.height
                val base = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    textSize = h * 0.55f
                }
                val measured = base.measureText(text)
                if (measured > 0f) {
                    base.textSize = minOf(h * 0.60f, w * 0.86f / measured * base.textSize)
                }
                base.shader = LinearGradient(
                    0f,
                    0f,
                    w,
                    0f,
                    intArrayOf(
                        android.graphics.Color.rgb(53, 230, 255),
                        android.graphics.Color.rgb(150, 110, 255),
                        android.graphics.Color.rgb(255, 62, 165),
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
                val x = (w - base.measureText(text)) / 2f
                val baseline = h / 2f + base.textSize * 0.34f
                val canvas = drawContext.canvas.nativeCanvas
                for ((widthScale, alpha) in listOf(0.085f to 46, 0.038f to 96)) {
                    val glow = Paint(base).apply {
                        strokeWidth = base.textSize * widthScale
                        this.alpha = alpha
                    }
                    canvas.drawText(text, x, baseline, glow)
                }
                val main = Paint(base).apply {
                    strokeWidth = base.textSize * 0.013f
                    this.alpha = 255
                }
                canvas.drawText(text, x, baseline, main)
            }
        }
    }
}
