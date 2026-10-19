package app.screenclock.ui.theme.earth

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import app.screenclock.domain.time.FaceTime
import androidx.compose.ui.graphics.Color
import app.screenclock.ui.theme.ClockScaffold
import app.screenclock.ui.theme.scene.EarthScene

/**
 * 地球景观：thin glowing digits over the night earth. The halo is a few wide
 * low-alpha strokes under one crisp outline.
 */
@Composable
internal fun EarthClock(
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
        EarthScene(modifier = Modifier.fillMaxSize())
        ClockScaffold(
            face = face,
            dateLine = dateLine,
            weatherLine = weatherLine,
            occasionLine = occasionLine,
            expanded = expanded,
            modifier = Modifier.fillMaxSize(),
            preview = preview,
            meridiemBelow = true,
            infoColor = Color(0xFFC9C2B6),
            onOccasionClick = onOccasionClick,
        ) { stacked, cardWidth, cardHeight, gap ->
            val areaWidth = if (stacked) cardWidth else cardWidth * 2 + gap
            val areaHeight = if (stacked) cardHeight * 2 + gap else cardHeight
            val text = "${face.hourDigits}:${face.minuteDigits}"
            Canvas(modifier = Modifier.requiredSize(areaWidth, areaHeight)) {
                val w = size.width
                val h = size.height
                val base = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
                    textSize = h * 0.60f
                }
                val measured = base.measureText(text)
                if (measured > 0f) {
                    base.textSize = minOf(h * 0.66f, w * 0.92f / measured * base.textSize)
                }
                val x = (w - base.measureText(text)) / 2f
                val baseline = h / 2f + base.textSize * 0.34f
                val canvas = drawContext.canvas.nativeCanvas
                for ((widthScale, alpha) in listOf(0.10f to 28, 0.05f to 60, 0.022f to 110)) {
                    val halo = Paint(base).apply {
                        strokeWidth = base.textSize * widthScale
                        color = android.graphics.Color.rgb(196, 220, 255)
                        this.alpha = alpha
                    }
                    canvas.drawText(text, x, baseline, halo)
                }
                val main = Paint(base).apply {
                    strokeWidth = base.textSize * 0.012f
                    color = android.graphics.Color.WHITE
                }
                canvas.drawText(text, x, baseline, main)
            }
        }
    }
}
