package app.screenclock.ui.theme.neon

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.theme.ClockScaffold
import app.screenclock.ui.theme.ThemeFont
import app.screenclock.ui.theme.splitLayout
import app.screenclock.ui.theme.rememberThemeTypeface
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
    val themeFont = rememberThemeTypeface(ThemeFont.Neon)
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
            Canvas(modifier = Modifier.requiredSize(areaWidth, areaHeight)) {
                val w = size.width
                val h = size.height
                val base = Paint().apply {
                    isAntiAlias = true
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    typeface = themeFont
                    textSize = h * 0.60f
                }
                val layout = splitLayout(base, face.hourDigits, face.minuteDigits, w, h)
                base.shader = LinearGradient(
                    0f,
                    0f,
                    w,
                    0f,
                    intArrayOf(
                        android.graphics.Color.rgb(90, 240, 255),
                        android.graphics.Color.rgb(190, 150, 255),
                        android.graphics.Color.rgb(255, 110, 190),
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
                val canvas = drawContext.canvas.nativeCanvas
                fun pass(paint: Paint) {
                    canvas.drawText(face.hourDigits, layout.hourX, layout.baseline, paint)
                    canvas.drawText(face.minuteDigits, layout.minuteX, layout.baseline, paint)
                    val dots = Paint(paint).apply { style = Paint.Style.FILL_AND_STROKE; strokeJoin = Paint.Join.MITER }
                    val half = layout.dotRadius * 1.1f
                    for (cy in listOf(layout.colonTop, layout.colonBottom)) {
                        canvas.drawRect(layout.colonX - half, cy - half, layout.colonX + half, cy + half, dots)
                    }
                }
                for ((widthScale, alpha) in listOf(0.07f to 40, 0.04f to 90)) {
                    pass(Paint(base).apply {
                        strokeWidth = base.textSize * widthScale
                        this.alpha = alpha
                    })
                }
                pass(Paint(base).apply {
                    strokeWidth = base.textSize * 0.024f
                    this.alpha = 255
                })
            }
        }
    }
}
