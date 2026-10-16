package app.screenclock.ui.theme.flip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import app.screenclock.domain.time.ColorwayId
import app.screenclock.domain.time.FaceTime
import app.screenclock.ui.Digit
import app.screenclock.ui.Flap
import app.screenclock.ui.theme.ClockScaffold

@Composable
internal fun FlipClock(
    face: FaceTime,
    dateLine: String?,
    weatherLine: String?,
    occasionLine: String?,
    colorway: ColorwayId,
    expanded: Boolean,
    animate: Boolean,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
    onOccasionClick: (() -> Unit)? = null,
) {
    val finish = flipFinish(colorway)
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
        FlapPair(
            hour = face.hourDigits,
            minute = face.minuteDigits,
            stacked = stacked,
            cardWidth = cardWidth,
            cardHeight = cardHeight,
            gap = gap,
            animate = animate,
            flapColor = finish.flap,
            digitColor = finish.digit,
        )
    }
}

/** Ink black is the only finish in v1. A later finish adds a branch here, not a new fold. */
private fun flipFinish(colorway: ColorwayId): FlipFinish {
    return when (colorway) {
        ColorwayId.InkBlack -> FlipFinish(flap = Flap, digit = Digit)
    }
}

private data class FlipFinish(val flap: Color, val digit: Color)

@Composable
private fun FlapPair(
    hour: String,
    minute: String,
    stacked: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    gap: Dp,
    animate: Boolean,
    flapColor: Color,
    digitColor: Color,
) {
    if (stacked) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlipCard(hour, animate, Modifier.size(cardWidth, cardHeight), flapColor, digitColor)
            Spacer(modifier = Modifier.height(gap))
            FlipCard(minute, animate, Modifier.size(cardWidth, cardHeight), flapColor, digitColor)
        }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            FlipCard(hour, animate, Modifier.size(cardWidth, cardHeight), flapColor, digitColor)
            Spacer(modifier = Modifier.width(gap))
            FlipCard(minute, animate, Modifier.size(cardWidth, cardHeight), flapColor, digitColor)
        }
    }
}
