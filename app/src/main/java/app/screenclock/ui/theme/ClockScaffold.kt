package app.screenclock.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.font.FontWeight
import app.screenclock.domain.time.DateSegment
import app.screenclock.ui.clock.LocalBurnInOffset
import app.screenclock.domain.time.DateSegmentKind
import app.screenclock.domain.time.dateSegments
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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
        val burnInOffset = LocalBurnInOffset.current
        Column(
            modifier = Modifier.graphicsLayer {
                val shift = burnInOffset()
                translationX = shift.xPx
                translationY = shift.yPx
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (dateLine != null && !dateBelow) {
                DateLine(dateLine, infoSize, infoColor, boardWidth)
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
                DateLine(dateLine, infoSize, infoColor, boardWidth)
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

internal fun dateLineCandidates(line: String): List<String> {
    val base = line.substringBefore(" · ")
    val pieces = base.split(' ').filter { it.isNotBlank() }
    if (pieces.isEmpty()) return listOf(line)
    val candidates = mutableListOf(line)
    if (base != line) candidates += base // Holiday is least important on a narrow screen.
    for (count in pieces.size - 1 downTo 1) {
        candidates += pieces.take(count).joinToString(" ")
    }
    val monthDay = pieces.first().substringAfter('年', missingDelimiterValue = pieces.first())
    candidates += monthDay
    return candidates.distinct()
}

internal fun fitDateLine(
    line: String,
    availableWidthPx: Int,
    sizesSp: List<Float>,
    measureWidthPx: (String, Float) -> Int,
): Pair<String, Float> {
    require(sizesSp.isNotEmpty())
    val candidates = dateLineCandidates(line)
    for (fontSp in sizesSp) {
        for (candidate in candidates) {
            if (measureWidthPx(candidate, fontSp) <= availableWidthPx) return candidate to fontSp
        }
    }
    // The final Text has explicit Ellipsis if even month/day cannot fit.
    return candidates.last() to sizesSp.last()
}

/**
 * The date line as typed chips: the solar date stands alone and bold, the lunar day sits in
 * an outlined pill tagged「农历」, the weekday in a soft filled pill, and a holiday in a
 * stronger pill. Shape and weight, not just words, tell the kinds apart at a glance.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DateLine(text: String, size: TextUnit, infoColor: Color, maxLineWidth: Dp) {
    val measurer = rememberTextMeasurer()
    val density = androidx.compose.ui.platform.LocalDensity.current
    BoxWithConstraints(modifier = Modifier.width(maxLineWidth), contentAlignment = Alignment.Center) {
        val selected = fitDateLine(text, constraints.maxWidth, listOf(size.value, size.value * 0.875f)) { candidate, fontSp ->
            val chips = dateSegments(candidate).size - 1
            val extra = with(density) { (fontSp * 1.7f * chips.coerceAtLeast(0)).sp.toPx() }
            measurer.measure(
                text = AnnotatedString(candidate),
                style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = fontSp.sp),
                maxLines = 1,
                softWrap = false,
            ).size.width + extra.toInt()
        }
        val fontSp = selected.second.sp
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(fontSp.value.dp * 0.55f, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(fontSp.value.dp * 0.3f),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            for (segment in dateSegments(selected.first)) {
                DateChip(segment, fontSp, infoColor)
            }
        }
    }
}

@Composable
private fun DateChip(segment: DateSegment, fontSize: TextUnit, infoColor: Color) {
    val pad = androidx.compose.ui.unit.Dp(fontSize.value * 0.45f)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
    val (label, weight, modifier) = when (segment.kind) {
        DateSegmentKind.Date -> Triple(segment.text, FontWeight.SemiBold, Modifier)
        DateSegmentKind.Lunar -> Triple(
            "农历 ${segment.text}", FontWeight.Normal,
            Modifier
                .border(1.dp, infoColor.copy(alpha = 0.55f), shape)
                .padding(horizontal = pad, vertical = pad * 0.25f),
        )
        DateSegmentKind.Weekday -> Triple(
            segment.text, FontWeight.Medium,
            Modifier
                .background(infoColor.copy(alpha = 0.16f), shape)
                .padding(horizontal = pad, vertical = pad * 0.25f),
        )
        DateSegmentKind.Holiday -> Triple(
            segment.text, FontWeight.Bold,
            Modifier
                .background(infoColor.copy(alpha = 0.34f), shape)
                .border(1.dp, infoColor.copy(alpha = 0.7f), shape)
                .padding(horizontal = pad, vertical = pad * 0.25f),
        )
    }
    Text(
        text = label,
        color = infoColor,
        fontFamily = FontFamily.SansSerif,
        fontSize = if (segment.kind == DateSegmentKind.Date) fontSize else fontSize * 0.88f,
        fontWeight = weight,
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
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
