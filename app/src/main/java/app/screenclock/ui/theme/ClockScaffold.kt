package app.screenclock.ui.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.screenclock.domain.time.FaceTime
import app.screenclock.domain.time.footerLine
import app.screenclock.ui.Info
import app.screenclock.ui.theme.flip.clockStacked
import app.screenclock.ui.theme.flip.flapLayout

/**
 * Shared slots for every theme: date above, hour and minute in the middle, one footer line below.
 * The footer is absent when both weather and occasion are absent.
 */
@Composable
internal fun ClockScaffold(
    face: FaceTime,
    dateLine: String?,
    weatherLine: String?,
    occasionLine: String?,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    digits: @Composable (stacked: Boolean, cardWidth: Dp, cardHeight: Dp, gap: Dp) -> Unit,
) {
    val pad = if (expanded) 32.dp else 16.dp
    val infoSize = if (expanded) 22.sp else 16.sp
    val footer = footerLine(weatherLine, occasionLine)
    BoxWithConstraints(
        modifier = modifier.padding(pad),
        contentAlignment = Alignment.Center,
    ) {
        val lineBlock = if (expanded) 56.dp else 44.dp
        val dateBlock = if (dateLine != null) lineBlock else 0.dp
        val footerBlock = if (footer != null) lineBlock else 0.dp
        val side = if (face.meridiem != null) {
            if (expanded) 80.dp else 64.dp
        } else {
            0.dp
        }
        val availHeight = (maxHeight - dateBlock - footerBlock).coerceAtLeast(1.dp)
        val stacked = clockStacked(maxWidth.value, maxHeight.value)
        val layout = flapLayout(
            width = maxWidth.value.coerceAtLeast(1f),
            height = availHeight.value,
            stacked = stacked,
            reserveSide = side.value,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (dateLine != null) {
                InfoLine(dateLine, infoSize)
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                digits(layout.stacked, layout.cardWidth.dp, layout.cardHeight.dp, layout.gap.dp)
                if (face.meridiem != null) {
                    Text(
                        text = face.meridiem,
                        color = Info,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = infoSize,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            if (footer != null) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoLine(footer, infoSize)
            }
        }
    }
}

@Composable
private fun InfoLine(text: String, size: androidx.compose.ui.unit.TextUnit) {
    Text(
        text = text,
        color = Info,
        fontFamily = FontFamily.SansSerif,
        fontSize = size,
        maxLines = 1,
        softWrap = false,
    )
}
