package app.screenclock.ui.theme.flip

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.screenclock.domain.time.FLIP_DURATION_MILLIS
import app.screenclock.domain.time.flipFrame
import app.screenclock.ui.Digit
import app.screenclock.ui.Flap
import app.screenclock.ui.Info
import app.screenclock.ui.Ink

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
    val fontSize = with(LocalDensity.current) { (cardHeight * 0.86f).toSp() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .align(if (top) Alignment.TopCenter else Alignment.BottomCenter),
    ) {
        Text(
            text = digits,
            color = digitColor,
            fontSize = fontSize,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(
                fontFeatureSettings = "tnum",
                lineHeight = fontSize,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
        )
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
        val radius = size.height * 0.04f
        drawCircle(color = Info, radius = radius, center = Offset(radius, y))
        drawCircle(color = Info, radius = radius, center = Offset(size.width - radius, y))
    }
}
