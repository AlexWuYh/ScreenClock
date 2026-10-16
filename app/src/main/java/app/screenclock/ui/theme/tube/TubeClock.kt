package app.screenclock.ui.theme.tube

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.TUBE_GLOW_MILLIS
import app.screenclock.domain.time.tubeFrame
import app.screenclock.ui.Amber
import app.screenclock.ui.TubeGlass
import app.screenclock.ui.theme.ClockScaffold

private const val REST_ALPHA = 0.62f

@Composable
internal fun TubeClock(
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
        TubeGroups(
            hour = face.hourDigits,
            minute = face.minuteDigits,
            stacked = stacked,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            gap = gap,
            animate = animate,
        )
    }
}

@Composable
private fun TubeGroups(
    hour: String,
    minute: String,
    stacked: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    gap: Dp,
    animate: Boolean,
) {
    if (stacked) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TubePair(hour, animate, Modifier.size(cardWidth, cardHeight))
            Spacer(modifier = Modifier.height(gap))
            TubePair(minute, animate, Modifier.size(cardWidth, cardHeight))
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            TubePair(hour, animate, Modifier.size(cardWidth, cardHeight))
            Spacer(modifier = Modifier.width(gap))
            TubePair(minute, animate, Modifier.size(cardWidth, cardHeight))
        }
    }
}

@Composable
private fun TubePair(
    digits: String,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = remember { TubeDigits(digits) }
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
        progress.snapTo(0f)
        state.previous = leaving
        state.current = digits
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = TUBE_GLOW_MILLIS, easing = LinearEasing),
        )
    }
    val glyphs = tubeFrame(
        from = state.previous,
        to = state.current,
        progress = progress.value,
        animate = animate && state.previous != state.current,
    )
    BoxWithConstraints(modifier) {
        val corner = minOf(maxWidth, maxHeight) * 0.18f
        val digitSize = maxHeight * 0.72f
        val shape = RoundedCornerShape(corner)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(TubeGlass)
                .border(width = 1.dp, color = Amber.copy(alpha = 0.28f), shape = shape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                glyphs.forEachIndexed { index, glyph ->
                    val cx = size.width * (index + 0.5f) / glyphs.size.coerceAtLeast(1)
                    val radius = size.minDimension * 0.62f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Amber.copy(alpha = 0.06f + 0.5f * glyph.brightness),
                                Color.Transparent,
                            ),
                            center = Offset(cx, size.height / 2f),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(cx, size.height / 2f),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                glyphs.forEach { glyph ->
                    Text(
                        text = glyph.char.toString(),
                        color = Amber.copy(alpha = REST_ALPHA + (1f - REST_ALPHA) * glyph.brightness),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        style = TextStyle(
                            fontFeatureSettings = "tnum",
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both,
                            ),
                        ),
                        fontSize = with(LocalDensity.current) { digitSize.toSp() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private class TubeDigits(start: String) {
    var previous by mutableStateOf(start)
    var current by mutableStateOf(start)
}
