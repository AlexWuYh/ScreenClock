package app.screenclock.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.screenclock.domain.occasion.occasionBarParts
import app.screenclock.domain.time.DatePosition
import app.screenclock.domain.time.DigitSize
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.footerLine
import app.screenclock.ui.FaceText
import app.screenclock.ui.theme.flip.clockStacked
import app.screenclock.ui.theme.flip.flapLayout

internal data class ClockLayoutPrefs(
    val datePosition: DatePosition = DatePosition.Above,
    val digitSize: DigitSize = DigitSize.Regular,
)

internal val LocalClockLayout = staticCompositionLocalOf { ClockLayoutPrefs() }

/**
 * Shared slots for every theme: date above, hour and minute in the middle, one footer line below.
 * The footer is absent when both weather and occasion are absent.
 * [preview] shrinks every block so a small box (the theme picker) keeps the flaps as its subject.
 * [meridiemBelow] puts the 上午/下午 mark under the digits instead of beside them;
 * [infoColor] tints the date, footer and meridiem so scene backdrops stay readable.
 */
@Composable
internal fun ClockScaffold(
    face: FaceTime,
    dateLine: String?,
    weatherLine: String?,
    occasionLine: String?,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
    meridiemBelow: Boolean = false,
    infoColor: Color = FaceText,
    onOccasionClick: (() -> Unit)? = null,
    digits: @Composable (stacked: Boolean, cardWidth: Dp, cardHeight: Dp, gap: Dp) -> Unit,
) {
    val layoutPrefs = LocalClockLayout.current
    val dateBelow = layoutPrefs.datePosition == DatePosition.Below
    val pad = when {
        preview -> 10.dp
        expanded -> 32.dp
        else -> 16.dp
    }
    val infoSize = when {
        preview -> 11.sp
        expanded -> 22.sp
        else -> 16.sp
    }
    val lineGap = if (preview) 6.dp else 16.dp
    val footer = footerLine(weatherLine, occasionLine)
    BoxWithConstraints(
        modifier = modifier.padding(pad),
        contentAlignment = Alignment.Center,
    ) {
        val boardWidth = maxWidth
        val lineBlock = when {
            preview -> 20.dp
            expanded -> 56.dp
            else -> 44.dp
        }
        val dateBlock = if (dateLine != null) lineBlock else 0.dp
        val footerBlock = if (footer != null) lineBlock else 0.dp
        val gapBlock = (if (dateLine != null) lineGap else 0.dp) + (if (footer != null) lineGap else 0.dp)
        val side = if (face.meridiem != null && !meridiemBelow) {
            when {
                preview -> 32.dp
                expanded -> 80.dp
                else -> 64.dp
            }
        } else {
            0.dp
        }
        val meridiemBlock = if (face.meridiem != null && meridiemBelow) {
            if (preview) 14.dp else 32.dp
        } else {
            0.dp
        }
        val availHeight = (maxHeight - dateBlock - footerBlock - gapBlock - meridiemBlock).coerceAtLeast(1.dp)
        val stacked = clockStacked(maxWidth.value, maxHeight.value)
        val layout = flapLayout(
            width = maxWidth.value.coerceAtLeast(1f),
            height = availHeight.value,
            stacked = stacked,
            reserveSide = side.value,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (dateLine != null && !dateBelow) {
                InfoLine(dateLine, infoSize, infoColor)
                Spacer(modifier = Modifier.height(lineGap))
            }
            if (face.meridiem != null && meridiemBelow) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ScaledDigits(layoutPrefs.digitSize.factor) {
                        digits(layout.stacked, layout.cardWidth.dp, layout.cardHeight.dp, layout.gap.dp)
                    }
                    Spacer(modifier = Modifier.height(if (preview) 2.dp else 6.dp))
                    Text(
                        text = face.meridiem,
                        color = infoColor,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = infoSize,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScaledDigits(layoutPrefs.digitSize.factor) {
                        digits(layout.stacked, layout.cardWidth.dp, layout.cardHeight.dp, layout.gap.dp)
                    }
                    if (face.meridiem != null) {
                        Text(
                            text = face.meridiem,
                            color = infoColor,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = infoSize,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
            if (dateLine != null && dateBelow) {
                Spacer(modifier = Modifier.height(lineGap))
                InfoLine(dateLine, infoSize, infoColor)
            }
            if (footer != null) {
                Spacer(modifier = Modifier.height(lineGap))
                FooterLine(
                    weatherLine = weatherLine,
                    occasionLine = occasionLine,
                    size = infoSize,
                    maxWidth = boardWidth,
                    infoColor = infoColor,
                    onOccasionClick = onOccasionClick,
                )
            }
        }
    }
}

@Composable
private fun ScaledDigits(scale: Float, content: @Composable () -> Unit) {
    Box(modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
        content()
    }
}

@Composable
private fun FooterLine(
    weatherLine: String?,
    occasionLine: String?,
    size: TextUnit,
    maxWidth: Dp,
    infoColor: Color,
    onOccasionClick: (() -> Unit)?,
) {
    val weather = weatherLine?.takeIf { it.isNotBlank() }
    val occasion = occasionLine?.takeIf { it.isNotBlank() }
    Row(
        modifier = Modifier.widthIn(max = maxWidth),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (weather != null) {
            InfoLine(weather, size, infoColor)
        }
        if (weather != null && occasion != null) {
            InfoLine(" · ", size, infoColor)
        }
        if (occasion != null) {
            OccasionBar(occasion, size, infoColor, onOccasionClick)
        }
    }
}

@Composable
private fun RowScope.OccasionBar(
    line: String,
    size: TextUnit,
    infoColor: Color,
    onClick: (() -> Unit)?,
) {
    val parts = occasionBarParts(line)
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        modifier = Modifier.weight(1f, fill = false).then(click),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = parts.lead,
            color = infoColor,
            fontFamily = FontFamily.SansSerif,
            fontSize = size,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (parts.count.isNotEmpty()) {
            Text(
                text = parts.count,
                color = infoColor,
                fontFamily = FontFamily.SansSerif,
                fontSize = size,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun InfoLine(
    text: String,
    size: TextUnit,
    infoColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = infoColor,
        fontFamily = FontFamily.SansSerif,
        fontSize = size,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}
