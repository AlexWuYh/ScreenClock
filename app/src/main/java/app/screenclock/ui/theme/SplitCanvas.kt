package app.screenclock.ui.theme

import android.graphics.Paint
import android.graphics.Rect

/** Where each piece of a canvas-drawn time goes. All x values are left edges. */
internal data class SplitLayout(
    val hourX: Float,
    val minuteX: Float,
    val colonX: Float,
    val colonTop: Float,
    val colonBottom: Float,
    val dotRadius: Float,
    val baseline: Float,
)

/**
 * Fits [hour] + colon + [minute] into [width] x [height] and centres the colon on the digits.
 * [paint] is resized in place to the chosen text size.
 */
internal fun splitLayout(paint: Paint, hour: String, minute: String, width: Float, height: Float): SplitLayout {
    val sample = "$hour$minute"
    paint.textSize = height * 0.60f
    fun total(): Float = paint.measureText(hour) + paint.measureText(minute) + paint.textSize * 0.55f
    val measured = total()
    if (measured > 0f) paint.textSize = minOf(height * 0.66f, width * 0.92f / measured * paint.textSize)
    val size = paint.textSize
    val gap = size * 0.2f
    val dot = size * 0.07f
    val hourW = paint.measureText(hour)
    val minuteW = paint.measureText(minute)
    val whole = hourW + gap + dot * 2f + gap + minuteW
    val left = (width - whole) / 2f
    val baseline = height / 2f + size * 0.34f
    val bounds = Rect()
    paint.getTextBounds(sample, 0, sample.length, bounds)
    val mid = baseline + (bounds.top + bounds.bottom) / 2f
    val span = (bounds.bottom - bounds.top) * 0.27f
    return SplitLayout(
        hourX = left,
        colonX = left + hourW + gap + dot,
        minuteX = left + hourW + gap + dot * 2f + gap,
        colonTop = mid - span,
        colonBottom = mid + span,
        dotRadius = dot,
        baseline = baseline,
    )
}
