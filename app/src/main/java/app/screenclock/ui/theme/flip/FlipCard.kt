package app.screenclock.ui.theme.flip

import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.screenclock.domain.time.FLIP_DURATION_MILLIS
import app.screenclock.domain.time.flipFrame
import app.screenclock.ui.Digit
import app.screenclock.ui.Flap
import app.screenclock.ui.Info
import app.screenclock.ui.Ink

private const val GLYPH_PROBE = "88"
private const val GLYPH_PROBE_SIZE = 100f

@Composable
internal fun FlipCard(
    digits: String,
    animate: Boolean,
    modifier: Modifier = Modifier,
    flapColor: Color = Flap,
    digitColor: Color = Digit,
) {
    val state = remember { FlapDigits(digits) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(digits, animate) {
        if (digits == state.current && progress.value == 1f) return@LaunchedEffect
        val leaving = state.current
        if (!animate || leaving == digits) {
            state.previous = digits
            state.current = digits
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        // Snap first so the new pair is not drawn settled for a frame.
        progress.snapTo(0f)
        state.previous = leaving
        state.current = digits
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = FLIP_DURATION_MILLIS, easing = LinearEasing),
        )
    }
    val frame = flipFrame(
        from = state.previous,
        to = state.current,
        progress = progress.value,
        animate = animate && state.previous != state.current,
    )
    BoxWithConstraints(modifier) {
        val corner = minOf(maxWidth, maxHeight) * FLAP_CORNER_FRACTION
        val cardHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(corner))
                .background(flapColor),
        ) {
            StaticHalf(frame.topBack, top = true, cardHeight = cardHeight, alignment = Alignment.TopCenter, flapColor = flapColor, digitColor = digitColor)
            StaticHalf(frame.bottomBack, top = false, cardHeight = cardHeight, alignment = Alignment.BottomCenter, flapColor = flapColor, digitColor = digitColor)
            frame.topFlap?.let { flap ->
                MovingHalf(
                    digits = flap,
                    top = true,
                    cardHeight = cardHeight,
                    alignment = Alignment.TopCenter,
                    flapColor = flapColor,
                    digitColor = digitColor,
                    turns = frame.topFlapTurns,
                    hingeAtBottom = true,
                )
            }
            frame.bottomFlap?.let { flap ->
                MovingHalf(
                    digits = flap,
                    top = false,
                    cardHeight = cardHeight,
                    alignment = Alignment.BottomCenter,
                    flapColor = flapColor,
                    digitColor = digitColor,
                    turns = frame.bottomFlapTurns,
                    hingeAtBottom = false,
                )
            }
            Hinge(Modifier.fillMaxSize())
        }
    }
}

private class FlapDigits(start: String) {
    var previous by mutableStateOf(start)
    var current by mutableStateOf(start)
}

@Composable
private fun BoxScope.StaticHalf(
    digits: String,
    top: Boolean,
    cardHeight: Dp,
    alignment: Alignment,
    flapColor: Color,
    digitColor: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .align(alignment)
            .clipToBounds()
            .background(flapColor),
    ) {
        Glyph(digits, top = top, cardHeight = cardHeight, digitColor = digitColor)
    }
}

@Composable
private fun BoxScope.MovingHalf(
    digits: String,
    top: Boolean,
    cardHeight: Dp,
    alignment: Alignment,
    flapColor: Color,
    digitColor: Color,
    turns: Float,
    hingeAtBottom: Boolean,
) {
    val shade = if (hingeAtBottom) turns else 1f - turns
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .align(alignment)
            .graphicsLayer {
                rotationX = if (hingeAtBottom) 90f * turns else -90f * (1f - turns)
                transformOrigin = TransformOrigin(
                    pivotFractionX = 0.5f,
                    pivotFractionY = if (hingeAtBottom) 1f else 0f,
                )
                cameraDistance = 16.dp.toPx()
            }
            .clipToBounds()
            .background(flapColor),
    ) {
        Glyph(digits, top = top, cardHeight = cardHeight, digitColor = digitColor)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Ink.copy(alpha = 0.45f * shade.coerceIn(0f, 1f))),
        )
    }
}

@Composable
private fun BoxScope.Glyph(
    digits: String,
    top: Boolean,
    cardHeight: Dp,
    digitColor: Color,
) {
    val argb = digitColor.toArgb()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .align(if (top) Alignment.TopCenter else Alignment.BottomCenter),
    ) {
        // Text clips once the glyph is larger than the font atlas. A path stays whole on a tablet.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = GLYPH_PROBE_SIZE
            fontFeatureSettings = "tnum"
            hinting = Paint.HINTING_OFF
            style = Paint.Style.FILL
            color = argb
        }
        val probe = Rect()
        paint.getTextBounds(GLYPH_PROBE, 0, GLYPH_PROBE.length, probe)
        if (probe.width() <= 0 || probe.height() <= 0) return@Canvas
        val fit = glyphFit(
            cardWidth = size.width,
            cardHeight = size.height,
            inkWidth = maxOf(probe.width().toFloat(), paint.measureText(GLYPH_PROBE)),
            inkHeight = probe.height().toFloat(),
            measuredSize = paint.textSize,
        )
        paint.textSize = GLYPH_PROBE_SIZE * fit.sizeScale
        val scaled = Rect()
        paint.getTextBounds(GLYPH_PROBE, 0, GLYPH_PROBE.length, scaled)
        val baseline = fit.inkTop - scaled.top
        val x = (size.width - paint.measureText(digits)) / 2f
        val path = Path()
        paint.getTextPath(digits, 0, digits.length, x, baseline, path)
        val canvas = drawContext.canvas.nativeCanvas
        if (path.isEmpty) {
            canvas.drawText(digits, x, baseline, paint)
        } else {
            canvas.drawPath(path, paint)
        }
    }
}

@Composable
private fun Hinge(modifier: Modifier) {
    Canvas(modifier) {
        val y = size.height / 2f
        val stroke = (size.height * 0.012f).coerceAtLeast(1f)
        drawLine(
            color = Ink,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
        )
    }
}
