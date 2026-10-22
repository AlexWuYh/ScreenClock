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
import app.screenclock.ui.theme.ThemeFont
import app.screenclock.ui.theme.splitLayout
import app.screenclock.ui.theme.rememberThemeTypeface
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
    val themeFont = rememberThemeTypeface(ThemeFont.Earth)
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
            Canvas(modifier = Modifier.requiredSize(areaWidth, areaHeight)) {
                val w = size.width
                val h = size.height
                val base = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    typeface = themeFont
                    textSize = h * 0.60f
                }
                val layout = splitLayout(base, face.hourDigits, face.minuteDigits, w, h)
                val canvas = drawContext.canvas.nativeCanvas
                fun pass(paint: Paint) {
                    canvas.drawText(face.hourDigits, layout.hourX, layout.baseline, paint)
                    canvas.drawText(face.minuteDigits, layout.minuteX, layout.baseline, paint)
                    val dots = Paint(paint).apply { style = Paint.Style.FILL_AND_STROKE }
                    canvas.drawCircle(layout.colonX, layout.colonTop, layout.dotRadius, dots)
                    canvas.drawCircle(layout.colonX, layout.colonBottom, layout.dotRadius, dots)
                }
                for ((widthScale, alpha) in listOf(0.10f to 28, 0.05f to 60, 0.022f to 110)) {
                    pass(Paint(base).apply {
                        strokeWidth = base.textSize * widthScale
                        color = android.graphics.Color.rgb(196, 220, 255)
                        this.alpha = alpha
                    })
                }
                pass(Paint(base).apply {
                    strokeWidth = base.textSize * 0.012f
                    color = android.graphics.Color.WHITE
                })
            }
        }
    }
}
