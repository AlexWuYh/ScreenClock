package app.screenclock.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/** This window only. System brightness stays untouched. */
internal const val NIGHT_DIM_BRIGHTNESS = 0.06f

/**
 * A small warm shift. RenderEffect needs API 31, so the matrix sits on an offscreen layer.
 */
internal val NightWarmMatrix = ColorMatrix(
    floatArrayOf(
        1.06f, 0.05f, 0.00f, 0f, 8f,
        0.00f, 0.94f, 0.00f, 0f, 2f,
        0.00f, 0.00f, 0.72f, 0f, 0f,
        0.00f, 0.00f, 0.00f, 1f, 0f,
    ),
)

internal fun Modifier.nightWarmTint(): Modifier = drawWithContent {
    val paint = Paint().apply {
        colorFilter = ColorFilter.colorMatrix(NightWarmMatrix)
    }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
        drawContent()
        canvas.restore()
    }
}
